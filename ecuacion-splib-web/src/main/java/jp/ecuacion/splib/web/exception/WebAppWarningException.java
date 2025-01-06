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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Locale;
import jp.ecuacion.lib.core.annotation.RequireNonnull;
import jp.ecuacion.lib.core.exception.checked.AppExceptionFields;
import jp.ecuacion.lib.core.exception.checked.AppWarningException;

/**
 * Adds buttonIdToPressOnConfirm feature on to AppWarningException.
 * 
 * <p>After the warning message is confirmed and "OK" button is pressed, 
 *     usually the submit is executed by javascript code of {@code form.submit()}.<br>
 *     But sometimes you want to execute extra javascript codes or something.
 *     In that case, you set {@code buttonIdToPressOnConfirm} 
 *     to designate the submit button to press instead of just being submitted by javascript.</p>
 */
public class WebAppWarningException extends AppWarningException {

  private static final long serialVersionUID = 1L;

  private String buttonIdToPressOnConfirm;

  /**
   * Construct a new instance.
   * 
   * @param messageId messageId
   * @param messageArgs messageArgs
   */
  public WebAppWarningException(@RequireNonnull String messageId, @Nonnull String... messageArgs) {
    this(Locale.getDefault(), messageId, messageArgs);
  }

  /**
   * Construct a new instance with the {@code locale}.
   * 
   * @param locale locale
   * @param messageId messageId
   * @param messageArgs messageArgs
   */
  public WebAppWarningException(@RequireNonnull Locale locale, @Nonnull String messageId,
      @Nonnull String... messageArgs) {
    super(locale, messageId, messageArgs);
  }

  @Override
  public @Nonnull WebAppWarningException fields(@Nullable AppExceptionFields fields) {
    return (WebAppWarningException) super.fields(fields);
  }

  @Override
  public @Nonnull AppExceptionFields fields() {
    return fields;
  }

  /**
   * Sets buttonIdToPressOnConfirm.
   * 
   * @param buttonIdToPressOnConfirm buttonIdToPressOnConfirm
   * @return WebAppWarningException
   */
  public @Nonnull WebAppWarningException buttonIdToPressOnConfirm(
      @Nullable String buttonIdToPressOnConfirm) {
    if (buttonIdToPressOnConfirm != null) {
      this.buttonIdToPressOnConfirm = buttonIdToPressOnConfirm;
    }

    return this;
  }

  /**
   * Returns buttonIdToPressOnConfirm.
   * 
   * @return buttonIdToPressOnConfirm
   */
  public @Nullable String buttonIdToPressOnConfirm() {
    return buttonIdToPressOnConfirm;
  }
}
