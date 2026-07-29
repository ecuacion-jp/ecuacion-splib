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

import jp.ecuacion.splib.web.controller.BuiltinAdminLoginController.BuiltinAdminLoginForm;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.record.BuiltinAdminLoginRecord;
import jp.ecuacion.splib.web.service.SplibGeneral1FormDoNothingService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Shows the login page for {@code ecuacion-splib}'s own built-in admin pages
 * (e.g. {@link ConfigController}).
 *
 * <p>Unlike {@link AdminLoginController}, which is an app-facing feature apps opt into and
 *     supply their own template for, this login page is bundled entirely inside
 *     {@code ecuacion-splib-web} (template included), since it exists solely to protect the
 *     library's own built-in pages. The POST that submits this form is handled directly by
 *     Spring Security's {@code loginProcessingUrl}, not by a controller method here; see
 *     {@code SplibBuiltinAdminSecurityConfig}.</p>
 *
 * <p>The function name is {@code builtinAdminLogin}, not {@code adminLogin} — function names
 *     must be unique across the app (see {@code ControllerContext#function()}), and
 *     {@code adminLogin} is already taken by the app-facing {@link AdminLoginController}, which
 *     is registered by default alongside this one. {@link #getDefaultHtmlPageName()} is
 *     overridden so the template file itself can still be named
 *     {@code ecuacion-splib-admin-login.html} rather than following from the function name.</p>
 */
@Controller
@Scope("prototype")
@RequestMapping("/ecuacion-splib/public/adminLogin")
public class BuiltinAdminLoginController extends SplibGeneral1FormController<BuiltinAdminLoginForm,
    SplibGeneral1FormDoNothingService<BuiltinAdminLoginForm>> {

  /**
   * Constructs a new instance.
   */
  public BuiltinAdminLoginController() {
    super("builtinAdminLogin");
  }

  @Override
  public String getDefaultHtmlPageName() {
    return "ecuacion-splib-admin-login";
  }

  /**
   * Stores data for the built-in admin login.
   */
  public static class BuiltinAdminLoginForm extends SplibGeneralForm {

    private BuiltinAdminLoginRecord builtinAdminLogin = new BuiltinAdminLoginRecord();

    /** Returns builtinAdminLogin. */
    public BuiltinAdminLoginRecord getBuiltinAdminLogin() {
      return builtinAdminLogin;
    }

    /** Sets builtinAdminLogin. */
    public void setBuiltinAdminLogin(BuiltinAdminLoginRecord builtinAdminLogin) {
      this.builtinAdminLogin = builtinAdminLogin;
    }
  }
}
