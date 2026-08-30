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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
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
    doFilterFromIp(filter, presentedApiKey, "127.0.0.1", response);
  }

  private void doFilterFromIp(SplibApiKeyAuthenticationFilter filter, String presentedApiKey,
      String remoteAddr, MockHttpServletResponse response) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/key/executeScript");
    request.setRemoteAddr(remoteAddr);
    request.addHeader(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, presentedApiKey);
    filter.doFilter(request, response, new MockFilterChain());
  }

  private void doFilterWithKeyId(SplibApiKeyAuthenticationFilter filter, String presentedApiKey,
      String apiKeyId, MockHttpServletResponse response) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/key/executeScript");
    request.setRemoteAddr("127.0.0.1");
    request.addHeader(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, presentedApiKey);
    request.addHeader(SplibApiKeyAuthenticationFilter.HEADER_API_KEY_ID, apiKeyId);
    filter.doFilter(request, response, new MockFilterChain());
  }

  private static MockEnvironment rateLimitEnv(int maxFailures) {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("jp.ecuacion.splib.rest.api-key.rate-limit.max-failures",
        Integer.toString(maxFailures));
    env.setProperty("jp.ecuacion.splib.rest.api-key.rate-limit.window-seconds", "60");
    env.setProperty("jp.ecuacion.splib.rest.api-key.rate-limit.lockout-seconds", "60");
    return env;
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

  @Test
  void constructorThrowsWhenRateLimitMaxFailuresIsNotPositive() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("jp.ecuacion.splib.rest.api-key.rate-limit.max-failures", "0");

    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> new SplibApiKeyAuthenticationFilter(new FixedValuesProvider(List.of()), env));

    assertTrue(Objects.requireNonNull(ex.getMessage()).contains("rate-limit.max-failures"));
  }

  @Test
  void locksOutAfterMaxFailuresFromTheSameIpEvenForTheCorrectKeyAfterward() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY))), rateLimitEnv(3));

    for (int i = 0; i < 3; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      doFilterFromIp(filter, "wrong-key", "10.0.0.1", response);
      assertEquals(401, response.getStatus());
    }

    // A 4th request from the same IP is rejected purely by the lockout, even with the correct
    // key — proving the lockout check happens before (and independently of) key comparison.
    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilterFromIp(filter, FIRST_KEY, "10.0.0.1", response);

    assertEquals(401, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void lockoutOnOneIpDoesNotAffectAnother() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY))), rateLimitEnv(3));

    for (int i = 0; i < 3; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      doFilterFromIp(filter, "wrong-key", "10.0.0.1", response);
      assertEquals(401, response.getStatus());
    }

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilterFromIp(filter, FIRST_KEY, "10.0.0.2", response);

    assertEquals(200, response.getStatus());
  }

  @Test
  void successfulMatchResetsTheFailureCountForThatIp() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY))), rateLimitEnv(3));

    // Two failures — one short of the 3-failure threshold — followed by a success.
    for (int i = 0; i < 2; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      doFilterFromIp(filter, "wrong-key", "10.0.0.1", response);
      assertEquals(401, response.getStatus());
    }
    MockHttpServletResponse successResponse = new MockHttpServletResponse();
    doFilterFromIp(filter, FIRST_KEY, "10.0.0.1", successResponse);
    assertEquals(200, successResponse.getStatus());

    // Two more failures afterward: if the count hadn't been reset by the success above, this
    // would be the 3rd and 4th failure and would already be locked out.
    for (int i = 0; i < 2; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      doFilterFromIp(filter, "wrong-key", "10.0.0.1", response);
      assertEquals(401, response.getStatus());
    }
    MockHttpServletResponse stillOkResponse = new MockHttpServletResponse();
    doFilterFromIp(filter, FIRST_KEY, "10.0.0.1", stillOkResponse);
    assertEquals(200, stillOkResponse.getStatus());
  }

  @Test
  void acceptsAnApiKeyIdWithOnlyAllowedCharacters() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY))));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilterWithKeyId(filter, FIRST_KEY, "caller-01_A", response);

    assertEquals(200, response.getStatus());
    assertEquals("caller-01_A",
        Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName());
  }

  @Test
  void rejectsAnApiKeyIdContainingAControlCharacter() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY))));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilterWithKeyId(filter, FIRST_KEY, "caller\nid", response);

    assertEquals(401, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void rejectsAnApiKeyIdExceedingTheMaxLength() throws Exception {
    SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
        new FixedValuesProvider(List.of(plain(FIRST_KEY))));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilterWithKeyId(filter, FIRST_KEY, "a".repeat(129), response);

    assertEquals(401, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void stillMatchesWhenExpectedValuesExceedsTheWarnThreshold() throws Exception {
    // Exercises the >10-entries warning path added for the bcrypt-CPU-amplification finding;
    // the match itself must still succeed (the warning is purely informational).
    List<SplibApiKeyExpectedValue> manyValues = new java.util.ArrayList<>();
    for (int i = 0; i < 15; i++) {
      manyValues.add(plain("decoy-key-" + i));
    }
    manyValues.add(plain(FIRST_KEY));

    SplibApiKeyAuthenticationFilter filter =
        new SplibApiKeyAuthenticationFilter(new FixedValuesProvider(manyValues));

    MockHttpServletResponse response = new MockHttpServletResponse();
    doFilter(filter, FIRST_KEY, response);

    assertEquals(200, response.getStatus());
  }
}
