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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Shows the login page for {@code ecuacion-splib}'s own built-in admin pages
 * (e.g. {@link ConfigController}).
 *
 * <p>Unlike {@link AdminLoginController}, which is an app-facing feature apps opt into and
 *     supply their own template for, this login page is bundled entirely inside
 *     {@code ecuacion-splib-web} (template included), since it exists solely to protect the
 *     library's own built-in pages. The POST that submits this form is handled directly by
 *     Spring Security's {@code loginProcessingUrl}; see
 *     {@code SplibBuiltinAdminSecurityConfig}.</p>
 */
@Controller
public class BuiltinAdminLoginController {

  /**
   * Shows the built-in admin login page.
   *
   * @return view name
   */
  @GetMapping("/ecuacion-splib/public/adminLogin/page")
  public String page() {
    return "ecuacion-splib-admin-login";
  }
}
