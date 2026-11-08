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

/**
 * Thrown by the app to reject an OAuth2 login.
 *
 * <p>When this is thrown from {@link SplibOauth2UserHandler#findOrCreateUser} or
 *     {@link SplibOauth2UserHandlerWithRegistrationConfirm#findUser}, splib doesn't log the user
 *     in and redirects to the login page with {@code ?oauth2Rejected}.
 *     To reject on the registration confirm page, call
 *     {@link SplibOauth2RegistrationConfirmService#rejectLogin} instead of throwing this.</p>
 *
 * <p>The message is written to the log as the reason of the rejection.
 *     It's not shown to the user.</p>
 */
public class SplibOauth2LoginRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new instance.
   *
   * @param reason the reason of the rejection, written to the log
   */
  public SplibOauth2LoginRejectedException(String reason) {
    super(reason);
  }
}
