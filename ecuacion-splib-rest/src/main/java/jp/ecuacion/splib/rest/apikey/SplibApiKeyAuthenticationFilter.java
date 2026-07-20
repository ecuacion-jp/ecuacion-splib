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
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
   * {@link SplibApiKeyExpectedValueProvider#getExpectedValue} as-is. See that method's javadoc.
   */
  public static final String HEADER_API_KEY_ID = "X-Api-Key-Id";

  /** The authority granted to a request that authenticates successfully via this filter. */
  private static final String API_KEY_AUTHORITY = "ROLE_API_KEY";

  private final DetailLogger detailLog = new DetailLogger(this);

  private final @Nullable SplibApiKeyExpectedValueProvider expectedValueProvider;
  private final SplibApiKeyComparisonMode comparisonMode;

  /**
   * Constructs a new instance.
   *
   * @param expectedValueProvider the application-supplied provider, or {@code null} if the
   *     application never registered one — every request is then rejected
   * @param comparisonMode how to compare the presented key against the provider's return value
   */
  public SplibApiKeyAuthenticationFilter(
      @Nullable SplibApiKeyExpectedValueProvider expectedValueProvider,
      SplibApiKeyComparisonMode comparisonMode) {
    this.expectedValueProvider = expectedValueProvider;
    this.comparisonMode = comparisonMode;
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

    String expectedValue =
        Objects.requireNonNull(expectedValueProvider).getExpectedValue(apiKeyId, presentedApiKey);
    if (expectedValue == null || !matches(presentedApiKey, expectedValue)) {
      detailLog.warn("apiKey mismatch on request to " + request.getRequestURI() + ".");
      reject(response);
      return;
    }

    UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
        .authenticated(apiKeyId != null ? apiKeyId : "api-key-client", null,
            List.of(new SimpleGrantedAuthority(API_KEY_AUTHORITY)));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }

  private boolean matches(String presentedApiKey, String expectedValue) {
    String comparisonValue =
        comparisonMode == SplibApiKeyComparisonMode.HASH ? sha256Hex(presentedApiKey)
            : presentedApiKey;

    // MessageDigest.isEqual() is used instead of String.equals() to avoid a timing attack.
    return MessageDigest.isEqual(comparisonValue.getBytes(StandardCharsets.UTF_8),
        expectedValue.getBytes(StandardCharsets.UTF_8));
  }

  private String sha256Hex(String value) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException ex) {
      // SHA-256 is guaranteed to be available on every conforming JDK.
      throw new AssertionError(ex);
    }

    byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  private void reject(HttpServletResponse response) throws IOException {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
        "Invalid or missing " + HEADER_API_KEY + ".");
  }
}
