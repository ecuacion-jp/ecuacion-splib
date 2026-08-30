/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.ecuacion.splib.rest.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver.Outcome;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver.Result;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests under {@code /api/ecuacion-splib/key/**} using the {@code X-Api-Key}
 * (and optional {@code X-Api-Key-Id}) request headers.
 *
 * <p>Parallels {@link SplibApiKeyAuthenticationFilter}, but is kept as a separate class so that
 *     {@code ecuacion-splib}'s own built-in endpoints (e.g. {@code SystemErrorController},
 *     {@code ClearPropertiesCacheController}) are gated independently of the application's own
 *     {@code /api/key/**} keys.</p>
 *
 * <p><strong>Credential property.</strong> Exactly one of the following must be set:</p>
 * <ul>
 * <li>{@code jp.ecuacion.splib.rest.builtin-api-key.password-plain} — the key itself.</li>
 * <li>{@code jp.ecuacion.splib.rest.builtin-api-key.password-bcrypt} — a bcrypt hash of it.</li>
 * </ul>
 * 
 * <p>See {@link SplibHashedPropertyResolver} for the shared convention this follows (also used by
 *     {@code ecuacion-splib-web}'s built-in admin login).</p>
 *
 * <p>Unlike a normal wrong-key rejection (generic 401, so a caller cannot distinguish a server
 *     misconfiguration from a wrong key), having <em>both</em> properties set at once is a
 *     misconfiguration only whoever controls {@code application.properties} could cause, so it
 *     is reported distinctly as a 500 naming the offending keys.</p>
 */
public class SplibBuiltinApiKeyAuthenticationFilter extends OncePerRequestFilter {

  /** Request header carrying the API key itself. Required on every request to this filter. */
  public static final String HEADER_API_KEY = SplibApiKeyHeaders.API_KEY;

  /**
   * Request header carrying an optional key identifier. Not used to look up which key to expect
   * (there is exactly one, fixed by {@code application.properties}) — only carried through as
   * the authenticated principal's name for logging/auditing purposes.
   *
   * <p>If present, it is validated by {@link SplibApiKeyIdValidator} (1-128 characters of
   *     alphanumeric, {@code -}, {@code _}) before use, bounding what a downstream application
   *     that logs or records {@code Authentication.getName()} ends up storing.</p>
   */
  public static final String HEADER_API_KEY_ID = SplibApiKeyHeaders.API_KEY_ID;

  /** The authority granted to a request that authenticates successfully via this filter. */
  private static final String API_KEY_AUTHORITY = "ROLE_BUILTIN_API_KEY";

  private static final String PROPERTY_PREFIX = "jp.ecuacion.splib.rest.builtin-api-key";

  private final DetailLogger detailLog = new DetailLogger(this);
  private final SplibApiKeyRateLimiter rateLimiter;

  /**
   * Constructs a new instance with the rate limiter's default thresholds (10 failures / 60
   * seconds / a 300-second lockout) — mainly for tests;
   * {@link jp.ecuacion.splib.rest.config.SplibRestSecurityConfig} instead
   * uses {@link #SplibBuiltinApiKeyAuthenticationFilter(Environment)} so the thresholds are
   * configurable.
   */
  public SplibBuiltinApiKeyAuthenticationFilter() {
    this(null);
  }

  /**
   * Constructs a new instance.
   *
   * @param env source for the {@code jp.ecuacion.splib.rest.builtin-api-key.rate-limit.*}
   *     properties (see {@link SplibApiKeyRateLimiter}), or {@code null} to use their defaults
   */
  public SplibBuiltinApiKeyAuthenticationFilter(@Nullable Environment env) {
    this.rateLimiter = SplibApiKeyRateLimiter.fromEnvironment(env, PROPERTY_PREFIX);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String remoteAddr = request.getRemoteAddr();
    if (rateLimiter.isLockedOut(remoteAddr)) {
      detailLog.warn("Request to " + request.getRequestURI() + " from " + remoteAddr
          + " rejected: too many recent apiKey mismatches from this address.");
      reject(response);
      return;
    }

    String presentedApiKey = request.getHeader(HEADER_API_KEY);
    final String apiKeyId = request.getHeader(HEADER_API_KEY_ID);

    if (presentedApiKey == null || presentedApiKey.isEmpty()) {
      detailLog.warn("Request to " + request.getRequestURI() + " is missing the " + HEADER_API_KEY
          + " header.");
      reject(response);
      return;
    }

    if (apiKeyId != null && !SplibApiKeyIdValidator.isValid(apiKeyId)) {
      detailLog.warn("Request to " + request.getRequestURI() + " has an invalid "
          + HEADER_API_KEY_ID + " header value (must be 1-128 characters of alphanumeric, "
          + "'-', '_').");
      reject(response);
      return;
    }

    Result result = SplibHashedPropertyResolver.authenticate(PROPERTY_PREFIX, presentedApiKey);

    if (result.getOutcome() == Outcome.MISCONFIGURED) {
      detailLog.error("More than one of " + result.getConfiguredKeys() + " is set; exactly "
          + "one is expected. Rejecting request to " + request.getRequestURI() + ".");
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "ecuacion-splib builtin API key is misconfigured: more than one of "
              + result.getConfiguredKeys() + " is set; exactly one is expected.");
      return;
    }

    if (result.getOutcome() != Outcome.MATCHED) {
      detailLog.warn("apiKey mismatch on request to " + request.getRequestURI() + ".");
      rateLimiter.recordFailure(remoteAddr);
      reject(response);
      return;
    }

    rateLimiter.recordSuccess(remoteAddr);

    UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
        .authenticated(apiKeyId != null ? apiKeyId : "builtin-api-key-client", null,
            List.of(new SimpleGrantedAuthority(API_KEY_AUTHORITY)));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }

  private void reject(HttpServletResponse response) throws IOException {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
        "Invalid or missing " + HEADER_API_KEY + ".");
  }
}
