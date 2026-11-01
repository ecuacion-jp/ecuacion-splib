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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;

@DisplayName("SwitchUserUtil")
class SwitchUserUtilTest {

  private static final Authentication ADMIN = UsernamePasswordAuthenticationToken
      .authenticated("admin1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

  /** The switched user's {@code UserDetails}, which does not have the switch user authority. */
  private static final UserDetails USER_DETAILS =
      User.withUsername("user1").password("").roles("ACCOUNT").build();

  /** The {@code Authentication} as {@code SwitchUserFilter} builds it while switched. */
  private static final Authentication SWITCHED =
      UsernamePasswordAuthenticationToken.authenticated(USER_DETAILS, null,
          List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT"),
              new SwitchUserGrantedAuthority("ROLE_PREVIOUS_ADMINISTRATOR", ADMIN)));

  private static final Authentication NOT_SWITCHED = UsernamePasswordAuthenticationToken
      .authenticated(USER_DETAILS, null, USER_DETAILS.getAuthorities());

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("switched: true, and the source is the administrator's Authentication")
  void switched() {
    assertThat(SwitchUserUtil.isSwitchedUser(SWITCHED)).isTrue();
    assertThat(SwitchUserUtil.getSwitchUserSource(SWITCHED)).isSameAs(ADMIN);
  }

  @Test
  @DisplayName("not switched: false, and the source is null")
  void notSwitched() {
    assertThat(SwitchUserUtil.isSwitchedUser(NOT_SWITCHED)).isFalse();
    assertThat(SwitchUserUtil.getSwitchUserSource(NOT_SWITCHED)).isNull();
  }

  @Test
  @DisplayName("null: false, and the source is null")
  void nullAuthentication() {
    assertThat(SwitchUserUtil.isSwitchedUser(null)).isFalse();
    assertThat(SwitchUserUtil.getSwitchUserSource(null)).isNull();
  }

  @Test
  @DisplayName("without arguments: looks at the current SecurityContext")
  void current() {
    assertThat(SwitchUserUtil.isSwitchedUser()).isFalse();

    SecurityContextHolder.getContext().setAuthentication(SWITCHED);
    assertThat(SwitchUserUtil.isSwitchedUser()).isTrue();
    assertThat(SwitchUserUtil.getSwitchUserSource()).isSameAs(ADMIN);

    SecurityContextHolder.getContext().setAuthentication(NOT_SWITCHED);
    assertThat(SwitchUserUtil.isSwitchedUser()).isFalse();
    assertThat(SwitchUserUtil.getSwitchUserSource()).isNull();
  }

  @Test
  @DisplayName("the switched user's UserDetails does not tell it is switched "
      + "(why the Authentication is looked at)")
  void userDetailsDoesNotTell() {
    assertThat(USER_DETAILS.getAuthorities())
        .noneMatch(a -> "ROLE_PREVIOUS_ADMINISTRATOR".equals(a.getAuthority()));
    assertThat(SWITCHED.getPrincipal()).isSameAs(USER_DETAILS);
  }
}
