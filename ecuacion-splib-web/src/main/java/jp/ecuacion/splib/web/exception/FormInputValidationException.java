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
package jp.ecuacion.splib.web.exception;

import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;



/**
 * Throws {@code FormInputValidationException}.
 * 
 * <p>This method validates the form again to get the appropriate messages.</p>
 * 
 * <p>Originally, I would have left it to spring mvc's validation, 
 * but the following two points are not acceptable.
 * 
 * <ol>
 * <li>the sorting order of multiple error messages changes each time you run it.</li>
 * <li>Validations such as @Size are not defined to ignore blank, 
 *     so validations such as @Size will return "false" 
 *     even if the value is blank and @NotEmpty is set to the variable.</li>
 * </ol>
 * 
 * <p>About 1, spring mvc standard validation error messages 
 *     are obtained from {@code #fields.errors} at thymeleaf.
 *     I tried to sort the list from java, {@code bindingResult.getFieldErrors()}
 *     is unmodifiable.</p>
 */
public class FormInputValidationException extends AppException {

  private static final long serialVersionUID = 1L;
  
  private SplibGeneralForm form;

  /**
   * Constructs a new instance.
   * 
   * @param form form
   */
  public FormInputValidationException(SplibGeneralForm form) {
    this.form = form;
  }
  
  public SplibGeneralForm getForm() {
    return form;
  }

  public void setForm(SplibGeneralForm form) {
    this.form = form;
  }
}
