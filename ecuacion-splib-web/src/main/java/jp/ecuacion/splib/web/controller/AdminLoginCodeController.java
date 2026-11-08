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

import jakarta.validation.Valid;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection;
import jp.ecuacion.splib.web.controller.AdminLoginCodeController.AdminLoginCodeForm;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.record.AdminLoginCodeRecord;
import jp.ecuacion.splib.web.service.SplibGeneral1FormDoNothingService;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Provides the page to enter the login code of the admin login
 *     ({@code /public/adminLoginCode/page}).
 *
 * <p>It's enabled when {@code jp.ecuacion.splib.web.admin-login.login-code.enabled} is
 *     {@code true}. See {@link SplibAdminLoginProtection}.</p>
 *
 * <p>{@code /public/admin*}{@code /**} can be accessed without login in the admin filter chain,
 *     so whether the request is waiting for the code is checked here: without the admin login, it
 *     redirects to the admin login page, and after the code is entered, to the url when the login
 *     procedure successfully ended. The template is {@code adminLoginCode.html}, which apps can
 *     replace with their own.</p>
 */
@Controller
@Scope("prototype")
@ConditionalOnProperty(name = SplibAdminLoginProtection.PROPERTY_LOGIN_CODE_ENABLED,
    havingValue = "true")
@RequestMapping("/public/adminLoginCode")
public class AdminLoginCodeController extends SplibGeneral1FormController<AdminLoginCodeForm,
    SplibGeneral1FormDoNothingService<AdminLoginCodeForm>> {

  private static final String ITEM_PROPERTY_PATH = "adminLoginCode." + AdminLoginCodeRecord.CODE;
  private static final String MSG_PREFIX = "jp.ecuacion.splib.web.adminLoginCode.message.";

  private final SplibAdminLoginProtection protection;

  /**
   * Constructs a new instance with {@code protection}.
   *
   * @param protection protection
   */
  public AdminLoginCodeController(SplibAdminLoginProtection protection) {
    super("adminLoginCode");
    this.protection = protection;
  }

  @Override
  public String page(Model model, AdminLoginCodeForm form,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    String redirect = redirectIfNotPending();
    if (redirect != null) {
      return redirect;
    }

    return super.page(model, form, loginUser);
  }

  /**
   * Verifies the code, and moves to the url when the login procedure successfully ended when it's
   *     right.
   *
   * @param model model
   * @param form form
   * @param loginUser loginUser
   * @return URL
   */
  @PostMapping(value = "action", params = "action=verify")
  public String verify(Model model, AdminLoginCodeForm form,
      @AuthenticationPrincipal UserDetails loginUser) {
    prepare(model, loginUser, form);
    String messageKey = switch (protection.verifyLoginCode(request, response,
        form.getAdminLoginCode().getCode())) {
      case OK -> null;
      case WRONG -> MSG_PREFIX + "wrong";
      case UNUSABLE -> MSG_PREFIX + "unusable";
      case TOO_MANY_FAILURES -> MSG_PREFIX + "tooManyFailures";
      case ACCOUNT_UNAVAILABLE -> {
        protection.logout(request, response);
        yield null;
      }
      case NOT_PENDING -> null;
    };

    throwViolationIfNotNull(messageKey);
    return nextPage();
  }

  /**
   * Sends a new code.
   *
   * @param model model
   * @param form form
   * @param loginUser loginUser
   * @return URL
   */
  @PostMapping(value = "action", params = "action=resend")
  public String resend(Model model, AdminLoginCodeForm form,
      @AuthenticationPrincipal UserDetails loginUser) {
    prepare(model, loginUser, form);
    String messageKey = switch (protection.resendLoginCode(request)) {
      case SENT -> null;
      case LIMIT_EXCEEDED -> MSG_PREFIX + "resendLimitExceeded";
      case SEND_FAILED -> MSG_PREFIX + "sendFailed";
      case ACCOUNT_UNAVAILABLE -> {
        protection.logout(request, response);
        yield null;
      }
      case NOT_PENDING -> null;
    };

    throwViolationIfNotNull(messageKey);
    return nextPage();
  }

  /** Returns the page to move to, by the state after the action. */
  private String nextPage() {
    String redirect = redirectIfNotPending();
    return redirect == null ? "redirect:" + SplibAdminLoginProtection.LOGIN_CODE_PAGE_PATH
        : redirect;
  }

  /** Returns the redirect when the request is not waiting for the code. */
  private @Nullable String redirectIfNotPending() {
    return switch (protection.getLoginCodeState(request)) {
      case PENDING -> null;
      case VERIFIED -> "redirect:" + protection.getDefaultSuccessUrl();
      case NOT_LOGGED_IN -> "redirect:" + SplibAdminLoginProtection.ADMIN_LOGIN_PAGE_PATH;
    };
  }

  private static void throwViolationIfNotNull(@Nullable String messageKey) {
    if (messageKey != null) {
      new Violations().add(new BusinessViolation(new String[] {ITEM_PROPERTY_PATH}, messageKey))
          .throwIfAny();
    }
  }

  /**
   * Stores data for the page to enter the login code.
   */
  public static class AdminLoginCodeForm extends SplibGeneralForm {

    @Valid
    private AdminLoginCodeRecord adminLoginCode = new AdminLoginCodeRecord();

    /** Returns adminLoginCode. */
    public AdminLoginCodeRecord getAdminLoginCode() {
      return adminLoginCode;
    }

    /** Sets adminLoginCode. */
    public void setAdminLoginCode(AdminLoginCodeRecord adminLoginCode) {
      this.adminLoginCode = adminLoginCode;
    }
  }
}
