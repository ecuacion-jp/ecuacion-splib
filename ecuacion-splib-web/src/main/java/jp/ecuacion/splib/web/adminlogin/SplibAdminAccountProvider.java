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
package jp.ecuacion.splib.web.adminlogin;

import org.jspecify.annotations.Nullable;

/**
 * Provides the state of an admin account to {@link SplibAdminLoginProtection}.
 *
 * <p>Implement it and register it as a bean when
 *     {@code jp.ecuacion.splib.web.admin-login.login-code.enabled} or
 *     {@code jp.ecuacion.splib.web.admin-login.session-check.enabled} is {@code true}.</p>
 */
@FunctionalInterface
public interface SplibAdminAccountProvider {

  /**
   * Finds the admin account of {@code loginId}, which can still log in.
   *
   * <p>Whether the account can log in (valid, mail address verified, not deleted, and so on)
   *     is decided by the app. It's called at the login, and on every request to
   *     {@code /admin/**} and while switched to another user when the session check is enabled,
   *     so it should be fast.</p>
   *
   * @param loginId the login ID, which is the username entered on the admin login page
   *     ({@code adminLogin.username}) and returned by {@code Authentication#getName()}
   * @return the state of the account, {@code null} if the account is not found or cannot log in
   */
  @Nullable
  SplibAdminAccountState find(String loginId);
}
