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
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Handles user lookup and creation for OAuth2 login.
 *
 * <p>Implement this interface as a Spring bean in your application to enable
 * Google / Apple social login. splib calls {@link #findOrCreateUser} after a
 * successful OAuth2 authentication and uses the returned {@link UserDetails}
 * to establish the security context.</p>
 */
public interface SplibOauth2UserHandler {

  /**
   * Finds an existing user by email, or creates one on first login.
   *
   * @param email    email address provided by the OAuth2 provider
   * @param name     display name provided by the provider (may be null for Apple
   *                 on subsequent logins)
   * @param provider provider identifier: {@code "GOOGLE"} or {@code "APPLE"}
   * @return {@link UserDetails} for the found or newly created user
   */
  UserDetails findOrCreateUser(String email, String name, String provider);

  /**
   * Called after the security context has been populated with the {@link UserDetails}.
   *
   * <p>Override this to set session attributes or perform any post-login
   * setup that is specific to your application (e.g. storing the user ID in the
   * session).</p>
   *
   * @param request     the current HTTP request
   * @param userDetails the {@link UserDetails} that was returned by
   *                    {@link #findOrCreateUser}
   */
  default void afterLoginSuccess(HttpServletRequest request, UserDetails userDetails) {}
}
