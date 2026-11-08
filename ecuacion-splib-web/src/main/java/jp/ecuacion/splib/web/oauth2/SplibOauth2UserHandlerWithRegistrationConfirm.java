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

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Provides the variant of {@link SplibOauth2UserHandler} that shows a registration confirm page
 * before registering a user who logs in for the first time.
 *
 * <p>Implement this instead of {@link SplibOauth2UserHandler} when the app must not register a
 *     user (nor write anything to the DB) until the user agrees on the confirm page.
 *     The flow is as follows:</p>
 * <ol>
 *   <li>After OAuth2 authentication succeeds, {@link #findUser} is called.</li>
 *   <li>If a user is found, the user is logged in as with {@link SplibOauth2UserHandler}.</li>
 *   <li>If not, the user is <b>not</b> logged in. The information from the provider is kept in
 *       the session as {@link SplibOauth2PendingRegistration} and the user is redirected to
 *       {@link #getRegistrationConfirmPage()}.</li>
 *   <li>The app's confirm page reads the information with
 *       {@link SplibOauth2RegistrationConfirmService#getPendingRegistration}. When the user
 *       agrees, the app registers the user and calls
 *       {@link SplibOauth2RegistrationConfirmService#loginAfterRegistration}, which logs the user
 *       in and returns the url to redirect to. When the user cancels, the app calls
 *       {@link SplibOauth2RegistrationConfirmService#discardPendingRegistration}.</li>
 * </ol>
 *
 * <p>{@link #findOrCreateUser} is not called by splib in this flow.</p>
 */
public interface SplibOauth2UserHandlerWithRegistrationConfirm extends SplibOauth2UserHandler {

  /**
   * Finds an existing user without creating one.
   *
   * <p>Find the user by {@link SplibOauth2UserInfo#provider()} and
   *     {@link SplibOauth2UserInfo#subject()}, not by the email address
   *     (see {@link SplibOauth2UserInfo}).</p>
   *
   * @param userInfo the information of the user received from the OAuth2 provider
   * @return {@link UserDetails} for the found user, {@code null} if the user is not registered
   */
  @Nullable
  UserDetails findUser(SplibOauth2UserInfo userInfo);

  /**
   * Returns the url of the registration confirm page, which is shown when {@link #findUser}
   * returns {@code null}.
   *
   * <p>Since the user is not logged in at this point,
   *     the page must be accessible without login (for example, under {@code /public/**}).</p>
   *
   * @return the url of the registration confirm page like
   *     {@code "/public/registrationConfirm/page"}
   */
  String getRegistrationConfirmPage();

  /**
   * Throws {@link UnsupportedOperationException} since it's not used in this flow.
   *
   * @param userInfo the information of the user received from the OAuth2 provider
   * @return never returns
   */
  @Override
  default UserDetails findOrCreateUser(SplibOauth2UserInfo userInfo) {
    throw new UnsupportedOperationException(
        "findOrCreateUser is not used in SplibOauth2UserHandlerWithRegistrationConfirm.");
  }
}
