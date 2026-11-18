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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * Unit tests for {@link TransactionTokenUtil}.
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

  @Test
  void consumeTokenRemovesHeldTokenOnlyOnce() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    String token = util.issueNewToken(request);

    assertThat(util.consumeToken(request, token)).isTrue();
    assertThat(tokenSet(request)).isEmpty();
    // Second submission of the same token (double submission) is rejected.
    assertThat(util.consumeToken(request, token)).isFalse();
  }

  @Test
  void consumeTokenRejectsUnknownToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    util.issueNewToken(request);

    assertThat(util.consumeToken(request, "unknown")).isFalse();
  }

  @Test
  void consumeTokenAcceptsWhenNoTokenSetInSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    // No session at all.
    assertThat(util.consumeToken(request, "any")).isTrue();
    // Session exists but no token has been issued.
    request.getSession();
    assertThat(util.consumeToken(request, "any")).isTrue();
  }

  /**
   * Regression test for {@code NoSuchElementException} thrown from {@code issueNewToken}:
   * concurrent requests of the same session used to corrupt the shared {@code LinkedHashSet}.
   */
  @Test
  void concurrentIssueAndConsumeOnSameSessionKeepsTokenSetConsistent() throws Exception {
    MockHttpSession session = new MockHttpSession();
    int threadCount = 8;
    int iterations = 5_000;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      futures.add(executor.submit(() -> {
        start.await();
        for (int i = 0; i < iterations; i++) {
          MockHttpServletRequest request = new MockHttpServletRequest();
          request.setSession(session);
          String token = util.issueNewToken(request);
          if (i % 2 == 0) {
            util.consumeToken(request, token);
          }
        }
        return null;
      }));
    }
    start.countDown();
    try {
      for (Future<?> future : futures) {
        // Throws ExecutionException if any thread failed (e.g. NoSuchElementException).
        future.get(60, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
    }

    @SuppressWarnings("unchecked")
    Set<String> tokenSet = (Set<String>) Objects.requireNonNull(
        session.getAttribute(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN));
    assertThat(tokenSet).hasSizeLessThanOrEqualTo(10);
    // size() must agree with the actually iterable elements.
    assertThat(new ArrayList<>(tokenSet)).hasSize(tokenSet.size());
  }

  @SuppressWarnings("unchecked")
  private static Set<String> tokenSet(MockHttpServletRequest request) {
    return (Set<String>) Objects.requireNonNull(request.getSession())
        .getAttribute(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN);
  }
}
