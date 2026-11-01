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
package jp.ecuacion.splib.webmvc.util;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;

/**
 * Provides utility methods on the switch user (an administrator logged in as another user, by
 *     {@link SwitchUserFilter}).
 *
 * <p>They look at the {@link Authentication}, not at the {@code UserDetails}: while switched,
 *     {@link SwitchUserFilter} adds a {@link SwitchUserGrantedAuthority}
 *     ({@code ROLE_PREVIOUS_ADMINISTRATOR}) holding the administrator's {@code Authentication}
 *     to the {@code Authentication}, but not to the switched user's {@code UserDetails} (the
 *     principal, e.g. {@code @AuthenticationPrincipal UserDetails}), which is the one loaded for
 *     that user. Checking the authorities of the {@code UserDetails} always answers "not
 *     switched".</p>
 *
 * <p>In Thymeleaf, {@code sec:authorize="hasAuthority('ROLE_PREVIOUS_ADMINISTRATOR')"} looks at
 *     the {@code Authentication} too, and gives the same answer as {@link #isSwitchedUser()}.
 *     </p>
 */
public class SwitchUserUtil {

  private SwitchUserUtil() {}

  /**
   * Returns whether the current user is a switched user.
   *
   * @return {@code true} if the {@code Authentication} in the current {@code SecurityContext} is
   *     a switched user
   */
  public static boolean isSwitchedUser() {
    return getSwitchUserSource() != null;
  }

  /**
   * Returns whether {@code authentication} is a switched user.
   *
   * @param authentication the authentication, may be {@code null}
   * @return {@code true} if {@code authentication} is a switched user
   */
  public static boolean isSwitchedUser(@Nullable Authentication authentication) {
    return getSwitchUserSource(authentication) != null;
  }

  /**
   * Returns the administrator's {@code Authentication} from before the switch user, for the
   *     current user.
   *
   * @return the administrator's {@code Authentication}, {@code null} if the current user is not
   *     a switched user
   */
  public static @Nullable Authentication getSwitchUserSource() {
    return getSwitchUserSource(SecurityContextHolder.getContext().getAuthentication());
  }

  /**
   * Returns the administrator's {@code Authentication} from before the switch user.
   *
   * @param authentication the authentication, may be {@code null}
   * @return the administrator's {@code Authentication}, {@code null} if {@code authentication}
   *     is {@code null} or not a switched user
   */
  public static @Nullable Authentication getSwitchUserSource(
      @Nullable Authentication authentication) {
    if (authentication == null) {
      return null;
    }

    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (authority instanceof SwitchUserGrantedAuthority switchUserAuthority) {
        return switchUserAuthority.getSource();
      }
    }

    return null;
  }
}
