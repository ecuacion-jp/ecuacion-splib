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

import java.util.regex.Pattern;

/**
 * Validates the {@code X-Api-Key-Id} header value before it is used, unmodified, as the
 * authenticated principal's name (see {@link SplibApiKeyAuthenticationFilter} and
 * {@link SplibBuiltinApiKeyAuthenticationFilter}).
 *
 * <p>Without this check, an attacker-controlled string (e.g. one containing newlines or other
 *     control characters) would flow into {@code Authentication.getName()} and from there into
 *     whatever a downstream application does with it (audit logs, database records, metrics
 *     tags, ...), some of which may not go through a control-character-safe logging path.
 *     Restricting it here, at the point it enters the security context, protects every such
 *     downstream use at once rather than relying on each one to sanitize it individually.</p>
 */
final class SplibApiKeyIdValidator {

  /**
   * Generous enough for any reasonable key identifier (a UUID, a short slug, ...) while still
   * bounding the value stored as the principal name.
   */
  private static final int MAX_LENGTH = 128;

  private static final Pattern ALLOWED_CHARS = Pattern.compile("[A-Za-z0-9_-]+");

  private SplibApiKeyIdValidator() {}

  /**
   * Checks whether {@code apiKeyId} is safe to use as the authenticated principal's name.
   *
   * @param apiKeyId the raw {@code X-Api-Key-Id} header value; never {@code null}
   * @return {@code true} if {@code apiKeyId} is 1-{@value #MAX_LENGTH} characters long and
   *     consists only of ASCII letters, digits, {@code -}, and {@code _}
   */
  static boolean isValid(String apiKeyId) {
    return apiKeyId.length() <= MAX_LENGTH && ALLOWED_CHARS.matcher(apiKeyId).matches();
  }
}
