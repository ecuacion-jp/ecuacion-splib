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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Verifies {@link SplibApiKeyAuthenticationFilter} accepts a request whose {@code X-Api-Key}
 * matches any one of several values returned by {@link SplibApiKeyExpectedValueProvider} — the
 * mechanism that lets each caller be issued its own key, revocable individually.
 */
class SplibApiKeyAuthenticationFilterTest {

  private static final String FIRST_KEY = "first-s3cr3t-key";
  private static final String SECOND_KEY = "second-s3cr3t-key";

  private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

  private static SplibApiKeyExpectedValue plain(String value) {
    return new SplibApiKeyExpectedValue(value, SplibApiKeyComparisonMode.PLAIN);
  }

  private static SplibApiKeyExpectedValue bcrypt(String value) {
    return new SplibApiKeyExpectedValue(Objects.requireNonNull(BCRYPT.encode(value)),
        SplibApiKeyComparisonMode.BCRYPT);
  }

  private static class FixedValuesProvider implements SplibApiKeyExpectedValueProvider {

    private final @Nullable List<SplibApiKeyExpectedValue> values;

    FixedValuesProvider(@Nullable List<SplibApiKeyExpectedValue> values) {
      this.values = values;
    }

    @Override
    public @Nullable List<SplibApiKeyExpectedValue> getExpectedValues(@Nullable String apiKeyId,
        String presentedApiKey) {
      return values;
    }
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void doFilter(SplibApiKeyAuthenticationFilter filter, String presentedApiKey,
      MockHttpServletResponse response) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/key/executeScript");
    request.addHeader(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, presentedApiKey);
    filter.doFilter(request, response, new MockFilterChain());
  }

  @Test
  void matchesFirstOfMultipleExpectedValues() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY), plain(SECOND_KEY))));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, FIRST_KEY, response);

    assertEquals(200, response.getStatus());
  }

  @Test
  void matchesSecondOfMultipleExpectedValues() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY), plain(SECOND_KEY))));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, SECOND_KEY, response);

    assertEquals(200, response.getStatus());
  }

  @Test
  void rejectsAValueNotAmongTheExpectedValues() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY), plain(SECOND_KEY))));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, "wrong-key", response);

    assertEquals(401, response.getStatus());
  }

  @Test
  void rejectsWhenProviderReturnsNull() throws Exception {
    SplibApiKeyAuthenticationFilter filter =
        new SplibApiKeyAuthenticationFilter(new FixedValuesProvider(null));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, FIRST_KEY, response);

    assertEquals(401, response.getStatus());
  }

  @Test
  void rejectsWhenProviderReturnsAnEmptyCollection() throws Exception {
    SplibApiKeyAuthenticationFilter filter =
        new SplibApiKeyAuthenticationFilter(new FixedValuesProvider(List.of()));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, FIRST_KEY, response);

    assertEquals(401, response.getStatus());
  }

  @Test
  void matchesAgainstABcryptHashedExpectedValue() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(bcrypt(FIRST_KEY), bcrypt(SECOND_KEY))));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, SECOND_KEY, response);

    assertEquals(200, response.getStatus());
  }

  @Test
  void matchesAPlainValueWhenAMixOfPlainAndBcryptValuesIsReturned() throws Exception {
    // A single provider call mixing modes is what makes migrating stored keys from plain text to
    // bcrypt one at a time possible: some rows are already bcrypt, others aren't converted yet.
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(bcrypt(FIRST_KEY), plain(SECOND_KEY))));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, SECOND_KEY, response);

    assertEquals(200, response.getStatus());
  }

  @Test
  void rejectsWhenExpectedValueProviderBeanIsAbsent() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(null);

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, FIRST_KEY, response);

    assertEquals(401, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
