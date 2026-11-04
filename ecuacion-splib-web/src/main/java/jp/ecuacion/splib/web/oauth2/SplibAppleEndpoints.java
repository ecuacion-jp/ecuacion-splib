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
package jp.ecuacion.splib.web.oauth2;

import java.net.URI;
import org.jspecify.annotations.Nullable;

/**
 * Detects Apple's endpoints.
 *
 * <p>Apple is detected by the host of its endpoints, not by the registrationId,
 *     so it works whatever registrationId the app uses.</p>
 */
final class SplibAppleEndpoints {

  /** Host of Apple's authorization and token endpoints. */
  static final String HOST = "appleid.apple.com";

  private SplibAppleEndpoints() {}

  /**
   * Returns whether the uri is one of Apple's endpoints.
   *
   * @param uri uri, may be {@code null}
   * @return {@code true} if the host of the uri is Apple's
   */
  static boolean isApple(@Nullable String uri) {
    if (uri == null) {
      return false;
    }

    try {
      return HOST.equalsIgnoreCase(URI.create(uri).getHost());

    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}
