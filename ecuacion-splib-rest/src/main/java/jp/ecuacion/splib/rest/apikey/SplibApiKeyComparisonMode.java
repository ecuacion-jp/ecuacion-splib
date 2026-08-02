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

/**
 * Selects how the value returned by
 * {@link SplibApiKeyExpectedValueProvider#getExpectedValues} is compared against the
 * client-presented {@code X-Api-Key} header value.
 *
 * <p>Selected application-wide via {@code jp.ecuacion.splib.rest.api-key.mode} (default
 *     {@code PLAIN}). A single application is assumed to use one mode consistently, so this is
 *     not configurable per endpoint or per key.</p>
 */
public enum SplibApiKeyComparisonMode {

  /**
   * {@link SplibApiKeyExpectedValueProvider#getExpectedValues} returns the keys themselves,
   * compared directly (in constant time, to avoid a timing attack) against the presented value.
   */
  PLAIN,

  /**
   * {@link SplibApiKeyExpectedValueProvider#getExpectedValues} returns the bcrypt hash of each
   * key, rather than the key itself, so the raw keys are never at rest anywhere the application
   * can read them back. Each presented header value is checked against every returned hash via
   * {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder#matches}, never
   * short-circuiting on the first match.
   *
   * <p>Because bcrypt is intentionally slow, a request is checked once per value returned by
   *     {@link SplibApiKeyExpectedValueProvider#getExpectedValues} — keep that collection small
   *     (e.g. narrow it down using the {@code X-Api-Key-Id} header) rather than returning every
   *     issued key on every request.</p>
   */
  BCRYPT
}
