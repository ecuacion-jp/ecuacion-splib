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
package jp.ecuacion.splib.web.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Provides the operations used by the app's registration confirm page
 * of {@link SplibOauth2UserHandlerWithRegistrationConfirm}.
 *
 * <p>This bean is activated only when a {@link SplibOauth2UserHandler} bean
 * is present in the application context.</p>
 */
@Service
@ConditionalOnBean(SplibOauth2UserHandler.class)
public class SplibOauth2RegistrationConfirmService {

  private final SplibOauth2AuthSuccessHandler successHandler;

  /**
   * Constructs a new instance.
   *
   * @param successHandler successHandler
   */
  public SplibOauth2RegistrationConfirmService(SplibOauth2AuthSuccessHandler successHandler) {
    this.successHandler = successHandler;
  }

  /**
   * Gets the information of the user waiting for the registration confirmation.
   *
   * @param request the current HTTP request
   * @return the pending registration,
   *     {@code null} if not exists (for example, the session has expired)
   */
  public @Nullable SplibOauth2PendingRegistration getPendingRegistration(
      HttpServletRequest request) {
    return SplibOauth2AuthSuccessHandler.getPendingRegistration(request);
  }

  /**
   * Discards the information of the user waiting for the registration confirmation.
   *
   * <p>Call this when the user cancels the registration.</p>
   *
   * @param request the current HTTP request
   */
  public void discardPendingRegistration(HttpServletRequest request) {
    SplibOauth2AuthSuccessHandler.removePendingRegistration(request);
  }

  /**
   * Logs in the user registered on the registration confirm page
   * and returns the url to redirect to.
   *
   * <p>Call this after the app has registered the user.
   *     The pending registration is discarded, and the same protections as the regular login
   *     (session id change and csrf token renewal) are applied.
   *     {@link SplibOauth2UserHandler#afterLoginSuccess} is also called.</p>
   *
   * <p>The returned url is the one saved before login (like the url in a notification mail),
   *     or the default success url if not saved. Return {@code "redirect:" + url}
   *     from the controller.</p>
   *
   * @param request the current HTTP request
   * @param response the current HTTP response
   * @param userDetails the {@link UserDetails} of the registered user
   * @return the url to redirect to
   */
  public String loginAfterRegistration(HttpServletRequest request, HttpServletResponse response,
      UserDetails userDetails) {
    return successHandler.loginAfterRegistration(request, response, userDetails);
  }
}
