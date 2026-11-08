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

import java.io.Serializable;

/**
 * Contains the information received from the OAuth2 provider for a user who is not registered
 * yet, kept in the session while the registration confirm page is shown.
 *
 * <p>Created by {@link SplibOauth2AuthSuccessHandler} when
 *     {@link SplibOauth2UserHandlerWithRegistrationConfirm#findUser} returns {@code null},
 *     and obtained by the app through
 *     {@link SplibOauth2RegistrationConfirmService#getPendingRegistration}.</p>
 *
 * <p>{@code userInfo} is the same as the one passed to
 *     {@link SplibOauth2UserHandlerWithRegistrationConfirm#findUser}, so Apple's first-login-only
 *     name is kept here as well.</p>
 *
 * @param userInfo the information of the user received from the provider
 */
public record SplibOauth2PendingRegistration(SplibOauth2UserInfo userInfo)
    implements Serializable {

  /** Session attribute key used to keep the pending registration. */
  static final String SESSION_KEY = SplibOauth2PendingRegistration.class.getName();
}
