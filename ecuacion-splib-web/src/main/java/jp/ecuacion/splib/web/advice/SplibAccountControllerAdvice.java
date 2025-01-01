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
package jp.ecuacion.splib.web.advice;

import jp.ecuacion.splib.core.form.record.SplibRecord;
import jp.ecuacion.splib.web.service.SplibAccountControllerAdviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Provides the function that puts logged-in accounts info to {@code Model}. 
 * Additionally, login-related procedure is done here.
 * 
 * <p>You can use the function by creating a class extends this.
 *     If the app you're creating doesn't have login feature, you don't need this.</p>
 * 
 * <p>If you store the account info to a database and use JPA to access the database, 
 *     Use {@code jp.ecuacion.splib.web.jpa.advice.SplibJpaAccountControllerAdvice}
 *     in {@code ecuacion-splib-web-jpa}.</p>
 */
public abstract class SplibAccountControllerAdvice {

  @Autowired
  SplibAccountControllerAdviceService service;

  /**
   * Sets account info into a {@code Model}.
   * 
   * @param model model
   * @param loginUser UserDetails
   */
  @ModelAttribute
  protected void setAccountInfo(Model model, @AuthenticationPrincipal UserDetails loginUser) {
    // login前の場合は終了。後続処理はloginUserが存在しない場合は不可のため。
    if (loginUser == null) {
      return;
    }

    executeForBoth();

    // systemAdmin roleが存在する場合はsystemAdmin扱いで処理
    if (loginUser.getAuthorities().stream().map(e -> e.getAuthority())
        .filter(auth -> auth.startsWith("ROLE_ADMIN")).toList().size() > 0) {

      SplibRecord adminLoginAcc = service.getAdminLoginAcc(loginUser);
      model.addAttribute("loginAcc", adminLoginAcc);

      executeForAdminAccount(adminLoginAcc);

    } else {
      SplibRecord loginAcc = service.getLoginAcc(loginUser);
      model.addAttribute("loginAcc", loginAcc);
      
      executeForAccount(loginAcc);
    }
  }

  /**
   * Additional procedure for both general accounts and admin accounts
   * can be define by overriding the method.
   */
  protected void executeForBoth() {

  }

  /**
   * Additional procedure for general accounts can be define by overriding the method.
   */
  protected void executeForAdminAccount(SplibRecord adminAcc) {
    
  }

  /**
   * Additional procedure for admin accounts can be define by overriding the method.
   */
  protected void executeForAccount(SplibRecord acc) {
    
  }
}
