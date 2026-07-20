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

import org.jspecify.annotations.Nullable;

/**
 * Supplies the expected value to compare a client-presented {@code X-Api-Key} header against,
 * for endpoints mapped under {@code /api/key/**}.
 *
 * <p>Implement this interface as a Spring bean in your application to require API-key
 * authentication for {@code /api/key/**}. If no bean of this type is registered, every request
 * to {@code /api/key/**} is rejected — there is no default "no key required" behavior for this
 * prefix, unlike {@code /api/public/**}.</p>
 *
 * <p>Where the expected value comes from (a fixed value in {@code application.properties}, a
 *     database row, ...) is entirely up to the implementation; splib only calls
 *     {@link #getExpectedValue} and compares the result against what the client sent.</p>
 */
public interface SplibApiKeyExpectedValueProvider {

  /**
   * Returns the value to compare {@code presentedApiKey} against, or {@code null} if no match
   * can be determined — the request is then rejected the same way a mismatched value would be,
   * so a caller cannot distinguish "no such key" from "wrong key".
   *
   * <p>Whether the returned value (and the comparison itself) is treated as plain text or as a
   *     SHA-256 hash is controlled application-wide by
   *     {@code jp.ecuacion.splib.rest.api-key.mode}; see {@link SplibApiKeyComparisonMode}.</p>
   *
   * @param apiKeyId the {@code X-Api-Key-Id} header value, or {@code null} if the client did not
   *     send one. Present so a per-client key scheme can be implemented — an identifier used to
   *     look up which record's expected value to check, analogous to an AWS access key ID or an
   *     HTTP Basic username, sent alongside the secret rather than derived from it. A
   *     single-shared-key implementation that has only one expected value regardless of who is
   *     calling can simply ignore this parameter.
   * @param presentedApiKey the {@code X-Api-Key} header value the client sent; never {@code null}
   *     or empty (already checked before this method is called)
   * @return the expected value to compare {@code presentedApiKey} against, or {@code null} to
   *     reject the request
   */
  @Nullable
  String getExpectedValue(@Nullable String apiKeyId, String presentedApiKey);
}
