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
package jp.ecuacion.splib.web.util.internal;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link TransactionTokenUtil#issueNewToken}.
 */
class TransactionTokenUtilTest {

  private final TransactionTokenUtil util = new TransactionTokenUtil();

  @Test
  void issueNewTokenAddsTokenToSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    String token = util.issueNewToken(request);

    Set<String> tokenSet = tokenSet(request);
    assertThat(tokenSet).containsExactly(token);
  }

  /**
   * Regression test for the unbounded-accumulation issue: a session that keeps issuing tokens
   * without ever submitting (e.g. repeated GETs of a {@code permitAll} page) must not grow the
   * token set past its upper bound.
   */
  @Test
  void issueNewTokenEvictsOldestTokenOnceLimitExceeded() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    List<String> issuedTokens = new ArrayList<>();
    for (int i = 0; i < 15; i++) {
      issuedTokens.add(util.issueNewToken(request));
    }

    Set<String> tokenSet = tokenSet(request);
    assertThat(tokenSet).hasSize(10);
    // The 10 most recently issued tokens survive; the 5 oldest are evicted.
    assertThat(tokenSet).containsExactlyElementsOf(issuedTokens.subList(5, 15));
  }

  @SuppressWarnings("unchecked")
  private static Set<String> tokenSet(MockHttpServletRequest request) {
    return (Set<String>) Objects.requireNonNull(request.getSession())
        .getAttribute(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN);
  }
}
