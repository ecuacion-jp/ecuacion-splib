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
package jp.ecuacion.splib.web.service;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jp.ecuacion.lib.core.annotation.RequireNonnull;
import jp.ecuacion.lib.core.exception.checked.AppExceptionFields;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;
import jp.ecuacion.splib.web.exception.WebAppWarningException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Is the service abstract class for general page of the page template.
 */
public abstract class SplibGeneralService {

  @Autowired
  private HttpServletRequest request;

  private DetailLogger detailLog = new DetailLogger(this);

  /** 
   * Provides the warning feature.
   * 
   * <p>By calling this method you can use the warning feature, 
   *     which means that the warning message is shown on the message box, 
   *     and by pressing OK button the warning is ignored at the next time.</p>
   *     
   * @param confirmedWarningMessageSet This is obtained from 
   *     {@code form.getConfirmedWarningMessageSet()}.
   * @param locale locale
   * @param buttonIdToPressOnConfirm buttonIdToPressOnConfirm, may be {@code null}.
   *     See {@link jp.ecuacion.splib.web.exception.WebAppWarningException}.
   * @param fields fields
   * @param msgId msgId
   * @param params params
   * @throws WebAppWarningException WebAppWarningException
   */
  protected void throwWarning(@RequireNonnull Set<String> confirmedWarningMessageSet,
      @RequireNonnull Locale locale, @Nullable String buttonIdToPressOnConfirm,
      @Nullable AppExceptionFields fields, @RequireNonnull String msgId,
      @RequireNonnull String... params) throws WebAppWarningException {

    if (!ObjectsUtil.paramRequireNonNull(confirmedWarningMessageSet).contains(msgId)) {
      throw new WebAppWarningException(locale, msgId, params).fields(fields)
          .buttonIdToPressOnConfirm(buttonIdToPressOnConfirm);
    }
  }

  // /**
  // * record内のlocalDate項目（String）をLocalDate形式で取得。
  // */
  // protected LocalDate localDate(String date) {
  // return (date == null || date.equals("")) ? null
  // : LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  // }

  /**
   * Obtains {@code DatetimeFormatParameters} instance.
   * 
   * <p>It's often used to construct a record.</p>
   */
  public DatetimeFormatParameters getParams() {
    return new SplibUtil().getParams(request);
  }

  /** 
   * Puts data needed to display a page.
   * 
   * <p>It's called from controllers and {@code SplibExceptionHandler} 
   *     right before showing a page.<br>
   *     In SplibGeneralService no procedures are defined in it. <br>
   *     It's not an abstract method because the implementation of this method is not mandatory.</p>
   *     
   * @param allFormList a list of forms
   * @param loginUser UserDetails
   */
  public void prepareForm(List<SplibGeneralForm> allFormList, UserDetails loginUser) {
    detailLog
        .debug("prepareForm(List<SplibGeneralForm> allFormList, UserDetails loginUser) called.");
  }
}
