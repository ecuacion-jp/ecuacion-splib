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

//@formatter:off
public abstract class SplibGeneral2FormsController
    <F1 extends SplibGeneralForm, F2 extends SplibGeneralForm, 
    S extends SplibGeneral2FormsService<F1, F2>> extends SplibGeneralController<S> {
  //@formatter:on

  public SplibGeneral2FormsController(@Nonnull String function) {
    super(function);
  }

  /** functionを指定したconstructor。functionだけは必須なのでconstructorの引数としている。 */
  protected SplibGeneral2FormsController(@Nonnull String function,
      @NonNull ControllerContext settings) {
    super(function, settings);
  }

  /**
   * postした際にsession timeoutだった場合はlogin画面に戻されるがその際もPOST通信になる場合があるなど、 場面によりget/post両方ありうるので両方記載しておく。
   */
  @RequestMapping(value = "page", method = {RequestMethod.GET, RequestMethod.POST})
  public String page(Model model, F1 form1, F2 form2,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    prepare(model, loginUser, form1, form2);
    getService().page(form1, form2, loginUser);
    return getReturnStringToShowPage();
  }

  /** 場面によりget/post両方ありうるので両方記載しておく。 */
  @RequestMapping(value = "action", params = "submitOnChangeToRefresh=true",
      method = {RequestMethod.GET, RequestMethod.POST})
  public String submitOnChangeToRefresh(Model model, F1 form1, F2 form2,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    prepare(model, loginUser, form1, form2);
    getService().prepareForm(form1, form2, loginUser);
    return getFunction() + StringUtils.capitalize(getSubFunction());
  }
}
