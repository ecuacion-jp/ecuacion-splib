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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests under {@code /api/key/**} using the {@code X-Api-Key} (and optional
 * {@code X-Api-Key-Id}) request headers.
 *
 * <p>All rejection paths (missing header, no {@link SplibApiKeyExpectedValueProvider} bean
 *     registered, no matching key, wrong key, rate-limit lockout) return the same generic 401
 *     response, so a caller cannot distinguish a server misconfiguration from a wrong key.
 *     Details are logged server-side only, and the presented key value itself is never
 *     logged.</p>
 *
 * <p>A key-mismatch failure counts against a per-source-IP lockout (see
 *     {@link SplibApiKeyRateLimiter}), configurable via {@code
 *     jp.ecuacion.splib.rest.api-key.rate-limit.max-failures} / {@code .window-seconds} /
 *     {@code .lockout-seconds} — this bounds both unlimited key-guessing and, when the comparison
 *     mode is {@code BCRYPT}, the CPU an attacker could otherwise burn by forcing a bcrypt
 *     comparison against every registered key on every single guess.</p>
 */
public class SplibApiKeyAuthenticationFilter extends OncePerRequestFilter {

  /** Request header carrying the API key itself. Required on every request to this filter. */
  public static final String HEADER_API_KEY = SplibApiKeyHeaders.API_KEY;

  /**
   * Request header carrying an optional key identifier, passed through to
   * {@link SplibApiKeyExpectedValueProvider#getExpectedValues} as-is. See that method's javadoc.
   *
   * <p>If present, it is validated by {@link SplibApiKeyIdValidator} (1-128 characters of
   *     alphanumeric, {@code -}, {@code _}) before use — it also becomes the authenticated
   *     principal's name, and this bounds what a downstream application that logs or records
   *     {@code Authentication.getName()} ends up storing.</p>
   */
  public static final String HEADER_API_KEY_ID = SplibApiKeyHeaders.API_KEY_ID;

  /** The authority granted to a request that authenticates successfully via this filter. */
  private static final String API_KEY_AUTHORITY = "ROLE_API_KEY";

  private static final String RATE_LIMIT_PROPERTY_PREFIX = "jp.ecuacion.splib.rest.api-key";

  /**
   * Above this many entries, a {@link SplibApiKeyExpectedValueProvider} is very likely returning
   * its whole key set instead of narrowing by {@code apiKeyId} as its javadoc instructs — logged
   * as a warning so the misuse (each guess forces a {@code BCRYPT} comparison against every
   * entry, an amplification an unauthenticated caller can trigger) is noticed early.
   */
  private static final int EXPECTED_VALUES_WARN_THRESHOLD = 10;

  private final DetailLogger detailLog = new DetailLogger(this);

  private final @Nullable SplibApiKeyExpectedValueProvider expectedValueProvider;
  private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder();
  private final SplibApiKeyRateLimiter rateLimiter;

  /**
   * Constructs a new instance with the rate limiter's default thresholds (10 failures / 60
   * seconds / a 300-second lockout) — mainly for tests;
   * {@link jp.ecuacion.splib.rest.config.SplibRestSecurityConfig} instead uses
   * {@link #SplibApiKeyAuthenticationFilter(SplibApiKeyExpectedValueProvider, Environment)}
   * so the thresholds are configurable.
   *
   * @param expectedValueProvider the application-supplied provider, or {@code null} if the
   *     application never registered one — every request is then rejected
   */
  public SplibApiKeyAuthenticationFilter(
      @Nullable SplibApiKeyExpectedValueProvider expectedValueProvider) {
    this(expectedValueProvider, null);
  }

  /**
   * Constructs a new instance.
   *
   * @param expectedValueProvider the application-supplied provider, or {@code null} if the
   *     application never registered one — every request is then rejected
   * @param env source for the {@code jp.ecuacion.splib.rest.api-key.rate-limit.*} properties (see
   *     the class javadoc), or {@code null} to use their defaults
   */
  public SplibApiKeyAuthenticationFilter(
      @Nullable SplibApiKeyExpectedValueProvider expectedValueProvider, @Nullable Environment env) {
    this.expectedValueProvider = expectedValueProvider;
    this.rateLimiter = SplibApiKeyRateLimiter.fromEnvironment(env, RATE_LIMIT_PROPERTY_PREFIX);
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
    String apiKeyId = request.getHeader(HEADER_API_KEY_ID);

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

    if (expectedValueProvider == null) {
      detailLog.warn("A request reached " + request.getRequestURI() + " but no "
          + "SplibApiKeyExpectedValueProvider bean is registered, so it is being rejected. "
          + "Register a bean implementing SplibApiKeyExpectedValueProvider to accept requests "
          + "under /api/key/**.");
      reject(response);
      return;
    }

    Collection<SplibApiKeyExpectedValue> expectedValues = Objects
        .requireNonNull(expectedValueProvider).getExpectedValues(apiKeyId, presentedApiKey);
    if (expectedValues != null && expectedValues.size() > EXPECTED_VALUES_WARN_THRESHOLD) {
      detailLog.warn("SplibApiKeyExpectedValueProvider#getExpectedValues returned "
          + expectedValues.size() + " entries for a single request, exceeding the expected "
          + "threshold of " + EXPECTED_VALUES_WARN_THRESHOLD + ". It should narrow the returned "
          + "values by apiKeyId rather than returning the whole key set; see its javadoc.");
    }

    SplibApiKeyExpectedValue matched =
        expectedValues == null ? null : findMatch(presentedApiKey, expectedValues);
    if (matched == null) {
      detailLog.warn("apiKey mismatch on request to " + request.getRequestURI() + ".");
      rateLimiter.recordFailure(remoteAddr);
      reject(response);
      return;
    }

    rateLimiter.recordSuccess(remoteAddr);

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority(API_KEY_AUTHORITY));
    matched.extraAuthorities().forEach(a -> authorities.add(new SimpleGrantedAuthority(a)));

    UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
        .authenticated(apiKeyId != null ? apiKeyId : "api-key-client", null, authorities);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }

  /**
   * Checks {@code presentedApiKey} against every value in {@code expectedValues}, always
   * comparing against all of them (never short-circuiting on a match) so that the total
   * comparison time does not itself hint at which entry — or how many entries — matched, and
   * returns the matched entry (so its {@link SplibApiKeyExpectedValue#extraAuthorities} can be
   * granted), or {@code null} if none matched. If more than one entry matches, the first one
   * encountered is returned.
   */
  private @Nullable SplibApiKeyExpectedValue findMatch(String presentedApiKey,
      Collection<SplibApiKeyExpectedValue> expectedValues) {
    byte[] presentedBytes = presentedApiKey.getBytes(StandardCharsets.UTF_8);

    SplibApiKeyExpectedValue matched = null;
    for (SplibApiKeyExpectedValue expectedValue : expectedValues) {
      boolean thisValueMatched = expectedValue.mode() == SplibApiKeyComparisonMode.BCRYPT
          ? bcryptPasswordEncoder.matches(presentedApiKey, expectedValue.value())
          // MessageDigest.isEqual() is used instead of String.equals() to avoid a timing attack.
          : MessageDigest.isEqual(presentedBytes,
              expectedValue.value().getBytes(StandardCharsets.UTF_8));
      matched = matched == null && thisValueMatched ? expectedValue : matched;
    }

    return matched;
  }

  private void reject(HttpServletResponse response) throws IOException {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
        "Invalid or missing " + HEADER_API_KEY + ".");
  }
}
