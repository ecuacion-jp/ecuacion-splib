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
 *     registered, no matching key, wrong key) return the same generic 401 response, so a caller
 *     cannot distinguish a server misconfiguration from a wrong key. Details are logged
 *     server-side only, and the presented key value itself is never logged.</p>
 */
public class SplibApiKeyAuthenticationFilter extends OncePerRequestFilter {

  /** Request header carrying the API key itself. Required on every request to this filter. */
  public static final String HEADER_API_KEY = "X-Api-Key";

  /**
   * Request header carrying an optional key identifier, passed through to
   * {@link SplibApiKeyExpectedValueProvider#getExpectedValues} as-is. See that method's javadoc.
   */
  public static final String HEADER_API_KEY_ID = "X-Api-Key-Id";

  /** The authority granted to a request that authenticates successfully via this filter. */
  private static final String API_KEY_AUTHORITY = "ROLE_API_KEY";

  private final DetailLogger detailLog = new DetailLogger(this);

  private final @Nullable SplibApiKeyExpectedValueProvider expectedValueProvider;
  private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder();

  /**
   * Constructs a new instance.
   *
   * @param expectedValueProvider the application-supplied provider, or {@code null} if the
   *     application never registered one — every request is then rejected
   */
  public SplibApiKeyAuthenticationFilter(
      @Nullable SplibApiKeyExpectedValueProvider expectedValueProvider) {
    this.expectedValueProvider = expectedValueProvider;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String presentedApiKey = request.getHeader(HEADER_API_KEY);
    String apiKeyId = request.getHeader(HEADER_API_KEY_ID);

    if (presentedApiKey == null || presentedApiKey.isEmpty()) {
      detailLog.warn("Request to " + request.getRequestURI() + " is missing the " + HEADER_API_KEY
          + " header.");
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
    SplibApiKeyExpectedValue matched =
        expectedValues == null ? null : findMatch(presentedApiKey, expectedValues);
    if (matched == null) {
      detailLog.warn("apiKey mismatch on request to " + request.getRequestURI() + ".");
      reject(response);
      return;
    }

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
