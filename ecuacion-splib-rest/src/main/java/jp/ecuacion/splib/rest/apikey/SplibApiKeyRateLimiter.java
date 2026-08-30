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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/**
 * Per-source-IP failure-count lockout, shared by {@link SplibApiKeyAuthenticationFilter} and
 * {@link SplibBuiltinApiKeyAuthenticationFilter} to blunt two things a bare generic-401 rejection
 * doesn't stop on its own: unlimited key-guessing attempts, and (when the comparison mode is
 * {@code BCRYPT}) the CPU an attacker can burn by forcing a bcrypt comparison against every
 * registered key on every single guess.
 *
 * <p>Entirely in-memory (JVM heap) — no external store is used. That means the count resets on
 *     restart and is not shared across instances behind a load balancer, but for this module's
 *     usual single-instance deployment that's an acceptable trade-off for not requiring a
 *     database this module otherwise has no use for.</p>
 *
 * <p>The client IP is read from {@link jakarta.servlet.ServletRequest#getRemoteAddr()}, which is
 *     the immediate TCP peer — if the app sits behind a reverse proxy, that's the proxy's own
 *     address for every request unless the proxy is explicitly trusted to rewrite it. Rather than
 *     parsing {@code X-Forwarded-For} here (a header any direct caller can also set, so trusting
 *     it blindly would let an attacker spoof a fresh IP on every request and bypass this
 *     entirely), deployments behind a trusted reverse proxy should enable Spring Boot's own
 *     {@code server.forward-headers-strategy=native} (or {@code framework}) instead, so {@code
 *     getRemoteAddr()} itself already reflects the real client IP by the time it reaches here.</p>
 */
final class SplibApiKeyRateLimiter {

  private static final int DEFAULT_MAX_FAILURES = 10;
  private static final long DEFAULT_WINDOW_SECONDS = 60;
  private static final long DEFAULT_LOCKOUT_SECONDS = 300;

  /** Sweep the map for stale entries every this many recorded failures, to bound its size. */
  private static final long SWEEP_INTERVAL = 1000;

  private final int maxFailures;
  private final long windowMillis;
  private final long lockoutMillis;

  private final ConcurrentHashMap<String, FailureRecord> recordsByIp = new ConcurrentHashMap<>();
  private final AtomicLong failureCallCount = new AtomicLong();

  private SplibApiKeyRateLimiter(int maxFailures, long windowSeconds, long lockoutSeconds) {
    this.maxFailures = maxFailures;
    this.windowMillis = TimeUnit.SECONDS.toMillis(windowSeconds);
    this.lockoutMillis = TimeUnit.SECONDS.toMillis(lockoutSeconds);
  }

  /**
   * Builds an instance from {@code propertyPrefix + ".rate-limit.*"} properties on {@code env},
   * falling back to sensible defaults for whichever of them (or {@code env} itself) is
   * {@code null}/unset — so this always returns a working, if permissively-defaulted, limiter
   * rather than requiring every caller (including tests constructing a filter directly) to
   * configure it.
   *
   * @param env the environment to read {@code <propertyPrefix>.rate-limit.max-failures} /
   *     {@code .window-seconds} / {@code .lockout-seconds} from, or {@code null} to use defaults
   *     for all three
   * @param propertyPrefix e.g. {@code "jp.ecuacion.splib.rest.api-key"}
   * @throws IllegalStateException if any configured value is not a positive number
   */
  static SplibApiKeyRateLimiter fromEnvironment(@Nullable Environment env,
      String propertyPrefix) {
    int maxFailures = env == null ? DEFAULT_MAX_FAILURES
        : env.getProperty(propertyPrefix + ".rate-limit.max-failures", Integer.class,
            DEFAULT_MAX_FAILURES);
    long windowSeconds = env == null ? DEFAULT_WINDOW_SECONDS
        : env.getProperty(propertyPrefix + ".rate-limit.window-seconds", Long.class,
            DEFAULT_WINDOW_SECONDS);
    long lockoutSeconds = env == null ? DEFAULT_LOCKOUT_SECONDS
        : env.getProperty(propertyPrefix + ".rate-limit.lockout-seconds", Long.class,
            DEFAULT_LOCKOUT_SECONDS);

    if (maxFailures <= 0) {
      throw new IllegalStateException("'" + propertyPrefix + ".rate-limit.max-failures' must be "
          + "a positive number, but was " + maxFailures + ".");
    }

    if (windowSeconds <= 0) {
      throw new IllegalStateException("'" + propertyPrefix + ".rate-limit.window-seconds' must "
          + "be a positive number of seconds, but was " + windowSeconds + ".");
    }

    if (lockoutSeconds <= 0) {
      throw new IllegalStateException("'" + propertyPrefix + ".rate-limit.lockout-seconds' must "
          + "be a positive number of seconds, but was " + lockoutSeconds + ".");
    }

    return new SplibApiKeyRateLimiter(maxFailures, windowSeconds, lockoutSeconds);
  }

  /**
   * Whether {@code ip} is currently locked out and should be rejected without even attempting a
   * key comparison.
   */
  boolean isLockedOut(String ip) {
    FailureRecord record = recordsByIp.get(ip);
    return record != null && record.lockedUntilMillis > System.currentTimeMillis();
  }

  /**
   * Records one failed key-comparison attempt from {@code ip}, locking it out once
   * {@code maxFailures} accumulate within {@code windowSeconds}.
   */
  void recordFailure(String ip) {
    long now = System.currentTimeMillis();
    recordsByIp.compute(ip, (key, existing) -> {
      if (existing == null || now - existing.windowStartMillis > windowMillis) {
        long lockedUntilMillis = 1 >= maxFailures ? now + lockoutMillis : 0;
        return new FailureRecord(now, now, 1, lockedUntilMillis);
      }

      int failureCount = existing.failureCount + 1;
      long lockedUntilMillis = failureCount >= maxFailures ? now + lockoutMillis
          : existing.lockedUntilMillis;
      return new FailureRecord(existing.windowStartMillis, now, failureCount, lockedUntilMillis);
    });

    // Piggybacks the cleanup on the same calls that grow the map (an attacker's own failed
    // guesses), rather than a scheduled background task, so a burst of failures both drives
    // growth and drives the sweep that bounds it.
    if (failureCallCount.incrementAndGet() % SWEEP_INTERVAL == 0) {
      sweepStaleEntries(now);
    }
  }

  /**
   * Clears any accumulated failure count for {@code ip} on a successful key match, so a caller
   * who mistyped their key a couple of times isn't left partway toward lockout afterward.
   */
  void recordSuccess(String ip) {
    recordsByIp.remove(ip);
  }

  /**
   * Removes entries that can no longer affect either {@link #isLockedOut} or the window logic in
   * {@link #recordFailure} — both the failure window and any lockout have long since expired.
   */
  private void sweepStaleEntries(long now) {
    long staleBefore = now - windowMillis - lockoutMillis;
    recordsByIp.entrySet().removeIf(
        entry -> entry.getValue().lastActivityMillis < staleBefore
            && entry.getValue().lockedUntilMillis < now);
  }

  /**
   * Immutable — {@link #recordFailure} replaces a key's value with a new instance (via
   * {@link ConcurrentHashMap#compute}) rather than mutating one in place. Relies on
   * {@link ConcurrentHashMap}'s own happens-before guarantee (a value read back via {@code get()}
   * reflects whatever {@code put}/{@code compute} last stored for that key) for cross-thread
   * visibility, so none of these fields need to be {@code volatile}.
   */
  private static final class FailureRecord {
    final long windowStartMillis;
    final long lastActivityMillis;
    final int failureCount;
    final long lockedUntilMillis;

    FailureRecord(long windowStartMillis, long lastActivityMillis, int failureCount,
        long lockedUntilMillis) {
      this.windowStartMillis = windowStartMillis;
      this.lastActivityMillis = lastActivityMillis;
      this.failureCount = failureCount;
      this.lockedUntilMillis = lockedUntilMillis;
    }
  }
}
