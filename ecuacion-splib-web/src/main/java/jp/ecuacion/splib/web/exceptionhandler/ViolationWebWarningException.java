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
package jp.ecuacion.splib.web.exceptionhandler;

import jp.ecuacion.lib.core.exception.ViolationWarningException;
import jp.ecuacion.lib.core.violation.Violations;

/**
 * Throw warning exception.
 */
public class ViolationWebWarningException extends ViolationWarningException {

  private static final long serialVersionUID = 1L;
  
  /**
   * The buttonId javascript presses when a user pressed okay on warning popup window.
   */
  private String buttonIdPressedIfConfirmed;

  /**
   * Constructs a new instance with the {@code Violations} that caused this exception.
   *
   * @param violations the violations that triggered the throw
   */
  public ViolationWebWarningException(Violations violations, String buttonIdPressedIfConfirmed) {
    super(violations);
    this.buttonIdPressedIfConfirmed = buttonIdPressedIfConfirmed;
  }

  public String getButtonIdPressedIfConfirmed() {
    return buttonIdPressedIfConfirmed;
  }
}
