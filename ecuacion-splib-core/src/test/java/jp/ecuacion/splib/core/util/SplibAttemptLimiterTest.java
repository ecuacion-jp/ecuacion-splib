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
package jp.ecuacion.splib.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SplibAttemptLimiter")
class SplibAttemptLimiterTest {

  /** A clock whose time is moved by the tests. */
  static class MutableClock extends Clock {

    private Instant now = Instant.parse("2026-11-08T00:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(@Nullable ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private final MutableClock clock = new MutableClock();

  private SplibAttemptLimiter limiter(int maxAttempts, int maxKeys) {
    return new SplibAttemptLimiter(maxAttempts, Duration.ofMinutes(15), maxKeys, clock);
  }

  private static void recordTimes(SplibAttemptLimiter limiter, String key, int times) {
    for (int i = 0; i < times; i++) {
      limiter.record(key);
    }
  }

  @Nested
  @DisplayName("isExceeded / record")
  class Counting {

    @Test
    @DisplayName("is not exceeded until maxAttempts are recorded, and is exceeded after")
    void exceededAtMaxAttempts() {
      SplibAttemptLimiter limiter = limiter(3, 100);
      recordTimes(limiter, "a@example.com", 2);
      assertThat(limiter.isExceeded("a@example.com")).isFalse();

      limiter.record("a@example.com");
      assertThat(limiter.isExceeded("a@example.com")).isTrue();
    }

    @Test
    @DisplayName("counts keys separately")
    void keysAreSeparate() {
      SplibAttemptLimiter limiter = limiter(1, 100);
      limiter.record("a@example.com");
      assertThat(limiter.isExceeded("b@example.com")).isFalse();
    }

    @Test
    @DisplayName("treats keys case-insensitively")
    void caseInsensitive() {
      SplibAttemptLimiter limiter = limiter(1, 100);
      limiter.record("A@Example.com");
      assertThat(limiter.isExceeded("a@example.com")).isTrue();
    }

    @Test
    @DisplayName("is not exceeded once the attempts are out of the window")
    void expires() {
      SplibAttemptLimiter limiter = limiter(2, 100);
      recordTimes(limiter, "a@example.com", 2);
      clock.advance(Duration.ofMinutes(15));
      assertThat(limiter.isExceeded("a@example.com")).isFalse();
    }

    @Test
    @DisplayName("does not count while exceeded, so that the end of the limit is not put off")
    void notCountedWhileExceeded() {
      SplibAttemptLimiter limiter = limiter(2, 100);
      recordTimes(limiter, "a@example.com", 2);
      clock.advance(Duration.ofMinutes(10));
      recordTimes(limiter, "a@example.com", 5);
      clock.advance(Duration.ofMinutes(5));
      assertThat(limiter.isExceeded("a@example.com")).isFalse();
    }

    @Test
    @DisplayName("reset removes the attempts of the key")
    void reset() {
      SplibAttemptLimiter limiter = limiter(1, 100);
      limiter.record("a@example.com");
      limiter.reset("A@example.com");
      assertThat(limiter.isExceeded("a@example.com")).isFalse();
    }
  }

  @Nested
  @DisplayName("the number of keys")
  class MaxKeys {

    @Test
    @DisplayName("trying many other keys does not reset the count of a key")
    void otherKeysDoNotPushOut() {
      SplibAttemptLimiter limiter = limiter(2, 3);
      recordTimes(limiter, "target@example.com", 2);
      for (int i = 0; i < 10; i++) {
        limiter.record("other" + i + "@example.com");
      }

      assertThat(limiter.isExceeded("target@example.com")).isTrue();
    }

    @Test
    @DisplayName("a new key is treated as exceeded while the keys are full")
    void newKeyExceededWhenFull() {
      SplibAttemptLimiter limiter = limiter(5, 2);
      limiter.record("a@example.com");
      limiter.record("b@example.com");
      assertThat(limiter.isExceeded("c@example.com")).isTrue();

      limiter.record("c@example.com");
      clock.advance(Duration.ofMinutes(15));
      // c was not recorded, so it is not exceeded once a and b are out of the window
      assertThat(limiter.isExceeded("c@example.com")).isFalse();
    }

    @Test
    @DisplayName("keys out of the window are removed to make room for a new key")
    void expiredKeysAreRemoved() {
      SplibAttemptLimiter limiter = limiter(5, 2);
      limiter.record("a@example.com");
      limiter.record("b@example.com");
      clock.advance(Duration.ofMinutes(15));
      limiter.record("c@example.com");
      assertThat(limiter.isExceeded("d@example.com")).isFalse();
    }

    @Test
    @DisplayName("a long key is accepted (kept as its hash)")
    void longKey() {
      SplibAttemptLimiter limiter = limiter(1, 100);
      String key = "x".repeat(1_000_000);
      limiter.record(key);
      assertThat(limiter.isExceeded(key)).isTrue();
    }
  }

  @ParameterizedTest
  @CsvSource({"0, 15, 1", "1, 0, 1", "1, 15, 0"})
  @DisplayName("rejects illegal arguments")
  void illegalArguments(int maxAttempts, int windowMinutes, int maxKeys) {
    assertThatThrownBy(() -> new SplibAttemptLimiter(maxAttempts,
        Duration.ofMinutes(windowMinutes), maxKeys, clock))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
