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
 * {@link SplibApiKeyExpectedValueProvider#getExpectedValue} is compared against the
 * client-presented {@code X-Api-Key} header value.
 *
 * <p>Selected application-wide via {@code jp.ecuacion.splib.rest.api-key.mode} (default
 *     {@code PLAIN}). A single application is assumed to use one mode consistently, so this is
 *     not configurable per endpoint or per key.</p>
 */
public enum SplibApiKeyComparisonMode {

  /**
   * {@link SplibApiKeyExpectedValueProvider#getExpectedValue} returns the key itself, compared
   * directly (in constant time, to avoid a timing attack) against the presented value.
   */
  PLAIN,

  /**
   * {@link SplibApiKeyExpectedValueProvider#getExpectedValue} returns the lowercase-hex SHA-256
   * digest of the key, rather than the key itself, so the raw key is never at rest anywhere the
   * application can read it back. The presented header value is hashed the same way before the
   * (constant-time) comparison.
   *
   * <p>To compute the value to store, hash the raw key on the command line, e.g.:</p>
   * <pre>{@code
   * # macOS
   * echo -n "your-api-key-here" | shasum -a 256
   *
   * # Linux
   * echo -n "your-api-key-here" | sha256sum
   *
   * # Cross-platform (OpenSSL)
   * echo -n "your-api-key-here" | openssl dgst -sha256
   * }</pre>
   *
   * <p>{@code -n} is required in all three: without it, echo appends a trailing newline that
   *     would be hashed too, producing a digest that never matches the presented key.</p>
   */
  HASH
}
