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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Counts attempts per key, and tells whether a key has reached the limit
 *     ({@code maxAttempts} times in {@code window}).
 *
 * <p>Used, for example, to limit the wrong passwords of a login ID, or the mails sent to an
 *     address. Call {@link #isExceeded(String)} before the attempt and reject it when
 *     {@code true}, and call {@link #record(String)} when the attempt is counted.</p>
 *
 * <p><strong>In memory.</strong> The counts are kept in this instance, so they are lost when the
 *     application restarts, and they are counted per server when the application runs on more
 *     than one server. Use one instance per kind of limit, as a singleton.</p>
 *
 * <p><strong>Keys.</strong> Keys are case-insensitive (like mail addresses). They are kept as
 *     their SHA-256 hashes, so a long key does not take much memory.</p>
 *
 * <p><strong>The number of keys.</strong> At most {@link #DEFAULT_MAX_KEYS} keys are kept. When it
 *     is full, the keys whose attempts are all out of the window are removed first. If it is
 *     still full (many keys are being tried in a short time, which means an attack), a key not
 *     kept yet is treated as exceeded and is not recorded. It does not remove a key still
 *     counted, because otherwise trying many other keys would reset the count of the key under
 *     attack.</p>
 *
 * <p>While a key has reached the limit, {@link #record(String)} does not count it, so that
 *     continuing to try does not put off the end of the limit.</p>
 *
 * <p>The methods are thread-safe.</p>
 */
public class SplibAttemptLimiter {

  /** The maximum number of keys kept by default. */
  public static final int DEFAULT_MAX_KEYS = 10000;

  private final int maxAttempts;
  private final Duration window;
  private final int maxKeys;
  private final Clock clock;
  private final Map<String, Deque<Instant>> attempts = new HashMap<>();

  /**
   * Constructs a new instance with {@code maxAttempts} and {@code window}.
   *
   * @param maxAttempts the number of attempts allowed in {@code window}, must be 1 or more
   * @param window the duration in which the attempts are counted, must be positive
   */
  public SplibAttemptLimiter(int maxAttempts, Duration window) {
    this(maxAttempts, window, DEFAULT_MAX_KEYS, Clock.systemUTC());
  }

  /**
   * Constructs a new instance with {@code maxAttempts}, {@code window}, {@code maxKeys} and
   *     {@code clock}.
   *
   * @param maxAttempts the number of attempts allowed in {@code window}, must be 1 or more
   * @param window the duration in which the attempts are counted, must be positive
   * @param maxKeys the maximum number of keys kept, must be 1 or more
   * @param clock the clock
   */
  public SplibAttemptLimiter(int maxAttempts, Duration window, int maxKeys, Clock clock) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be 1 or more: " + maxAttempts);
    }

    if (window.isNegative() || window.isZero()) {
      throw new IllegalArgumentException("window must be positive: " + window);
    }

    if (maxKeys < 1) {
      throw new IllegalArgumentException("maxKeys must be 1 or more: " + maxKeys);
    }

    this.maxAttempts = maxAttempts;
    this.window = window;
    this.maxKeys = maxKeys;
    this.clock = clock;
  }

  /**
   * Returns whether {@code key} has reached the limit.
   *
   * @param key the key, like a login ID
   * @return {@code true} if {@code key} has reached the limit, or if the keys kept are full and
   *     {@code key} is not kept
   */
  public synchronized boolean isExceeded(String key) {
    String hashed = hash(key);
    Deque<Instant> times = getAlive(hashed);
    if (times == null) {
      return isFull();
    }

    return times.size() >= maxAttempts;
  }

  /**
   * Counts an attempt of {@code key}.
   *
   * <p>It does nothing while {@code key} has reached the limit,
   *     or when the keys kept are full and {@code key} is not kept.</p>
   *
   * @param key the key, like a login ID
   */
  public synchronized void record(String key) {
    String hashed = hash(key);
    Deque<Instant> times = getAlive(hashed);
    if (times == null) {
      if (isFull()) {
        return;
      }

      times = new ArrayDeque<>();
      attempts.put(hashed, times);
    }

    if (times.size() < maxAttempts) {
      times.addLast(clock.instant());
    }
  }

  /**
   * Removes the attempts counted for {@code key}.
   *
   * @param key the key, like a login ID
   */
  public synchronized void reset(String key) {
    attempts.remove(hash(key));
  }

  /** Returns the attempts in the window, removing the key if there are none. */
  private @Nullable Deque<Instant> getAlive(String hashed) {
    Deque<Instant> times = attempts.get(hashed);
    if (times == null) {
      return null;
    }

    removeExpired(times);
    if (times.isEmpty()) {
      attempts.remove(hashed);
      return null;
    }

    return times;
  }

  /** Returns whether no more keys can be kept, after removing the keys out of the window. */
  private boolean isFull() {
    if (attempts.size() < maxKeys) {
      return false;
    }

    Iterator<Deque<Instant>> it = attempts.values().iterator();
    while (it.hasNext()) {
      Deque<Instant> times = it.next();
      removeExpired(times);
      if (times.isEmpty()) {
        it.remove();
      }
    }

    return attempts.size() >= maxKeys;
  }

  private void removeExpired(Deque<Instant> times) {
    Instant limit = clock.instant().minus(window);
    while (!times.isEmpty() && !times.peekFirst().isAfter(limit)) {
      times.removeFirst();
    }
  }

  private static String hash(String key) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(key.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));

    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
