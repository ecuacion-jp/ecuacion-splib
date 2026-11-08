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

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Makes the OIDC login fail when the email address is not provided or not verified.
 *
 * <p>The email address is passed to the app as {@link SplibOauth2UserInfo#email()}
 *     and may be registered or used to contact the user,
 *     so an unverified one, which may belong to someone else, must not be accepted.
 *     {@code email_verified} not set is treated the same as {@code false}.
 *     Google and Apple always set it when the email address is provided.</p>
 *
 * <p>The failure is handled by {@link SplibOauth2AuthFailureHandler} like the other failures
 *     of the OAuth2 login.</p>
 */
public class SplibOauth2OidcUserService extends OidcUserService {

  /** The error code used when the provider doesn't provide the email address. */
  public static final String EMAIL_NOT_PROVIDED_ERROR_CODE = "email_not_provided";

  /** The error code used when the email address is not verified by the provider. */
  public static final String EMAIL_NOT_VERIFIED_ERROR_CODE = "email_not_verified";

  /**
   * Constructs a new instance.
   */
  public SplibOauth2OidcUserService() {}

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser user = super.loadUser(userRequest);

    String email = user.getEmail();
    if (email == null || email.isBlank()) {
      throw new OAuth2AuthenticationException(new OAuth2Error(EMAIL_NOT_PROVIDED_ERROR_CODE,
          "The email address is not provided. Check that the 'email' scope is requested.",
          null));
    }

    if (!Boolean.TRUE.equals(user.getEmailVerified())) {
      throw new OAuth2AuthenticationException(new OAuth2Error(EMAIL_NOT_VERIFIED_ERROR_CODE,
          "The email address is not verified by the provider. email_verified: "
              + user.getEmailVerified(),
          null));
    }

    return user;
  }
}
