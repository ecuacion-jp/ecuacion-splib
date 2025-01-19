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

import jp.ecuacion.splib.web.form.AdminLoginForm;
import jp.ecuacion.splib.web.service.SplibGeneral1FormDoNothingService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Provides controller for admin login.
 */
@Controller
@Scope("prototype")
@RequestMapping("/public/adminLogin")
public class AdminLoginController extends
    SplibGeneral1FormController<AdminLoginForm, SplibGeneral1FormDoNothingService<AdminLoginForm>> {

  /**
   * Constructs a new instance.
   */
  public AdminLoginController() {
    super("adminLogin");
  }
}
