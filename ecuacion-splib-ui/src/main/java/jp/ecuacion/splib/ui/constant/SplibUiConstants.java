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
package jp.ecuacion.splib.ui.constant;

/**
 * Provides constants shared by ecuacion-splib's UI modules (web, cli, etc.).
 */
public class SplibUiConstants {

  /** {@code messageId} used for the {@code BusinessViolation} raised by ecuacion-splib's
   *  own required-field ({@code notEmpty}) check, which runs independently of Jakarta
   *  Validation's {@code @NotEmpty}. */
  public static final String MESSAGE_KEY_NOT_EMPTY =
      "jakarta.validation.constraints.NotEmpty.message";
}
