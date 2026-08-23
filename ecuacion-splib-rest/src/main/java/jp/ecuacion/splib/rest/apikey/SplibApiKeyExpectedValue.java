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

import java.util.Collection;
import java.util.List;

/**
 * One value {@link SplibApiKeyExpectedValueProvider#getExpectedValues} returns, paired with how
 * that particular value is encoded.
 *
 * <p>Carrying the mode alongside each value (rather than as one application-wide setting) lets a
 *     single provider return a mix of {@link SplibApiKeyComparisonMode#PLAIN} and
 *     {@link SplibApiKeyComparisonMode#BCRYPT} values at once — e.g. while migrating stored keys
 *     from plain text to bcrypt one row at a time, or issuing new keys as bcrypt while older
 *     already-issued ones remain plain until rotated.</p>
 *
 * @param value the value to compare the presented {@code X-Api-Key} against, encoded as indicated
 *     by {@code mode}
 * @param mode how {@code value} is encoded, and therefore how it must be compared
 * @param extraAuthorities additional authorities granted on top of {@code ROLE_API_KEY} when this
 *     particular value is the one that matched — e.g. to give one issued key more or fewer
 *     privileges than another. Empty by default.
 */
public record SplibApiKeyExpectedValue(String value, SplibApiKeyComparisonMode mode,
    Collection<String> extraAuthorities) {

  /**
   * Constructs a new instance with no {@link #extraAuthorities}, so the matching request is
   * granted only {@code ROLE_API_KEY}.
   *
   * @param value the value to compare the presented {@code X-Api-Key} against, encoded as
   *     indicated by {@code mode}
   * @param mode how {@code value} is encoded, and therefore how it must be compared
   */
  public SplibApiKeyExpectedValue(String value, SplibApiKeyComparisonMode mode) {
    this(value, mode, List.of());
  }
}
