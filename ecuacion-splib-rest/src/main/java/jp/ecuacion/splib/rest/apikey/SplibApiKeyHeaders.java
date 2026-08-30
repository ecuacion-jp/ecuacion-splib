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
 * Header name literals shared by {@link SplibApiKeyAuthenticationFilter} and
 * {@link SplibBuiltinApiKeyAuthenticationFilter} — both authenticate via the same two headers,
 * differing only in what key(s) they check the presented value against.
 *
 * <p>Each filter still exposes its own {@code HEADER_API_KEY} / {@code HEADER_API_KEY_ID}
 *     constant (backed by the values here) rather than pointing callers at this class directly,
 *     since the meaning of {@code HEADER_API_KEY_ID} differs slightly between the two — see each
 *     filter's own javadoc for that.</p>
 */
final class SplibApiKeyHeaders {

  /** Request header carrying the API key itself. */
  static final String API_KEY = "X-Api-Key";

  /** Request header carrying an optional key identifier. */
  static final String API_KEY_ID = "X-Api-Key-Id";

  private SplibApiKeyHeaders() {}
}
