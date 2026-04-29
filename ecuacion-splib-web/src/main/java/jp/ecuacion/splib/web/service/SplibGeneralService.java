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

import org.jspecify.annotations.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;
import jp.ecuacion.splib.web.exceptionhandler.ViolationWebWarningException;
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

  @Autowired
  private SplibUtil util;

  private DetailLogger detailLog = new DetailLogger(this);

  /**
   * Provides the warning feature.
   *
   * <p>By calling this method you can use the warning feature,
   * which means that the warning message is shown on the message box,
   * and by pressing OK button the warning is ignored at the next time.</p>
   *
   * @param confirmedWarningMessageSet This is obtained from
   *     {@code form.getConfirmedWarningMessageSet()}.
   * @param buttonIdToPressOnConfirm the ID of the button to press when the user confirms
   *     the warning, may be {@code null}.
   *     See {@link ViolationWebWarningException}.
   * @param msgId msgId
   * @param params params
   */
  protected void throwWarning(Set<String> confirmedWarningMessageSet,
      @Nullable String buttonIdToPressOnConfirm, String msgId, String... params) {

    if (!ObjectsUtil.requireNonNull(confirmedWarningMessageSet).contains(msgId)) {
      Violations violations = new Violations().add(msgId, params);
      if (buttonIdToPressOnConfirm != null) {
        throw new ViolationWebWarningException(violations, buttonIdToPressOnConfirm);
      }
      violations.throwWarningIfAny();
    }
  }

  /**
   * Obtains {@code DatetimeFormatParameters} instance.
   * 
   * <p>It's often used to construct a record.</p>
   */
  public DatetimeFormatParameters getParams() {
    return util.getParams(request);
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
  public void prepareForm(List<SplibGeneralForm> allFormList,
      @Nullable UserDetails loginUser) {
    detailLog
        .debug("prepareForm(List<SplibGeneralForm> allFormList, UserDetails loginUser) called.");
  }
}
