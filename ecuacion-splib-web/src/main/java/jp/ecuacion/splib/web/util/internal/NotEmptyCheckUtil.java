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
package jp.ecuacion.splib.web.util.internal;

import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Decides whether a value is empty for {@code HtmlItem#notEmpty()} checks.
 */
public class NotEmptyCheckUtil {

  private NotEmptyCheckUtil() {}

  /**
   * Returns {@code true} if the value is regarded as empty in {@code notEmpty()} checks.
   *
   * <p>{@code null} and an empty string are empty.
   *     A {@link MultipartFile} is empty when no file is chosen, which is judged by its empty
   *     original filename (browsers send a file part with {@code filename=""} in that case).
   *     {@link MultipartFile#isEmpty()} is deliberately not used, since it is also {@code true}
   *     for a chosen zero-byte file, which should be accepted as an uploaded file.</p>
   *
   * @param value the value to check
   * @return {@code true} if the value is empty
   */
  public static boolean isEmpty(@Nullable Object value) {
    if (value == null) {
      return true;
    }

    if (value instanceof String s) {
      return s.isEmpty();
    }

    if (value instanceof MultipartFile file) {
      String filename = file.getOriginalFilename();
      return filename == null || filename.isEmpty();
    }

    return false;
  }
}
