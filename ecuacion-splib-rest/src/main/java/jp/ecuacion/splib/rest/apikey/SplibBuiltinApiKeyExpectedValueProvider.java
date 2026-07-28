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
import org.jspecify.annotations.Nullable;

/**
 * Supplies the expected values to compare a client-presented {@code X-Api-Key} header against,
 * for {@code ecuacion-splib}'s own built-in endpoints mapped under
 * {@code /api/ecuacion-splib/key/**} (e.g. {@code SystemErrorController},
 * {@code ClearPropertiesCacheController}).
 *
 * <p>This is deliberately a separate type from {@link SplibApiKeyExpectedValueProvider}, which
 *     backs the application's own {@code /api/key/**} namespace, so that the two key sets
 *     (and the {@code jp.ecuacion.splib.rest.builtin-api-key.mode} comparison mode selecting
 *     plain-text vs. hashed comparison for this one) are registered and rotated independently.
 *     If no bean of this type is registered, every request to
 *     {@code /api/ecuacion-splib/key/**} is rejected.</p>
 *
 * <p>More than one valid value can be returned — e.g. one per issued token — so that a single
 *     leaked or retired key can be dropped without invalidating the others. Where the values come
 *     from (fixed values in {@code application.properties}, rows in a database, ...) is entirely
 *     up to the implementation; splib only calls {@link #getExpectedValues} and accepts the
 *     request if {@code presentedApiKey} matches any of the returned values.</p>
 */
public interface SplibBuiltinApiKeyExpectedValueProvider {

  /**
   * Returns the values to compare {@code presentedApiKey} against; the request is authenticated
   * if it matches any of them. Return {@code null} or an empty collection if no match can be
   * determined — the request is then rejected the same way a mismatched value would be, so a
   * caller cannot distinguish "no such key" from "wrong key".
   *
   * <p>Whether the returned values (and the comparison itself) are treated as plain text or as a
   *     SHA-256 hash is controlled application-wide by
   *     {@code jp.ecuacion.splib.rest.builtin-api-key.mode}; see {@link SplibApiKeyComparisonMode}.
   *     </p>
   *
   * @param apiKeyId the {@code X-Api-Key-Id} header value, or {@code null} if the client did not
   *     send one. Present so a per-client key scheme can be implemented — an identifier used to
   *     look up which record's expected value(s) to check, analogous to an AWS access key ID or
   *     an HTTP Basic username, sent alongside the secret rather than derived from it. An
   *     implementation that accepts any of a shared pool of keys regardless of who is calling can
   *     simply ignore this parameter.
   * @param presentedApiKey the {@code X-Api-Key} header value the client sent; never {@code null}
   *     or empty (already checked before this method is called)
   * @return the values to compare {@code presentedApiKey} against, or {@code null} to reject the
   *     request
   */
  @Nullable
  Collection<String> getExpectedValues(@Nullable String apiKeyId, String presentedApiKey);
}
