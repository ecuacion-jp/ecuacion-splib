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
package jp.ecuacion.splib.web.controller;

import jakarta.annotation.Nonnull;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.service.SplibGeneral2FormsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Is the abstract general controller with 2 Forms. 
 * 
 * @param <F1> SplibGeneralForm
 * @param <F2> SplibGeneralForm
 * @param <S> SplibGeneralService
 */
//@formatter:off
public abstract class SplibGeneral2FormsController
    <F1 extends SplibGeneralForm, F2 extends SplibGeneralForm, 
    S extends SplibGeneral2FormsService<F1, F2>> extends SplibGeneralController<S> {
  //@formatter:on

  /**
   * Construct a new instance with {@code function}.
   * 
   * @param function function
   */
  public SplibGeneral2FormsController(@Nonnull String function) {
    super(function);
  }

  /**
   * Construct a new instance with {@code function}, {@code settings}.
   * 
   * @param function function
   * @param settings settings
   */
  protected SplibGeneral2FormsController(@Nonnull String function,
      @NonNull ControllerContext settings) {
    super(function, settings);
  }

  /**
   * Provides the default method to show page.
   * 
   * <p>Since Occuring an system error is not prefereble 
   *     when this method is accessed with {@code POST} method,
   *     this method also receives {@code POST} method.
   *     (It happens mostly By being redirected the POST request to this page. 
   *     (like accessing to the login needed page but the session timed out)</p>
   *     
   * @param model model
   * @param form1 form
   * @param form2 form
   * @param loginUser loginUser
   * @return URL
   * @throws Exception Exception
   */
  @RequestMapping(value = "page", method = {RequestMethod.GET, RequestMethod.POST})
  public String page(Model model, F1 form1, F2 form2,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    prepare(model, loginUser, form1, form2);
    getService().page(form1, form2, loginUser);
    getService().prepareForm(form1, form2, loginUser);
    return getDefaultHtmlPageName();
  }

  /**
   * Provides the default method to refresh selections of the dropdowns, and so on.
   * 
   * <p>This is called in the following situation.<br>
   *     There are two dropdowns, one of them (A) has selections of the names of prefectures,
   *     and others (B) the names of cities.<br>
   *     When A is selected, refresh the selections of B which names belongs to prefecture A.<br>
   *     To realize this, page should be submitted when A is selected, but not validated
   *     since it's stile in the middle of the input a form.</p>
   * 
   * <p>Since Occuring an system error is not prefereble 
   *     when this method is accessed with {@code POST} method,
   *     this method also receives {@code POST} method.
   *     (It happens mostly By being redirected the POST request to this page. 
   *     (like accessing to the login needed page but the session timed out)</p>
   *     
   * @param model model
   * @param form1 form
   * @param form2 form
   * @param loginUser loginUser
   * @return html page
   * @throws Exception Exception
   */
  @RequestMapping(value = "action", params = "submitOnChangeToRefresh=true",
      method = {RequestMethod.GET, RequestMethod.POST})
  public String submitOnChangeToRefresh(Model model, F1 form1, F2 form2,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    prepare(model, loginUser, form1, form2);
    getService().prepareForm(form1, form2, loginUser);
    return getFunction() + StringUtils.capitalize(getSubFunction());
  }
}
