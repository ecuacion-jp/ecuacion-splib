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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.splib.web.adminlogin.AdminLoginTestSupport.CapturingMailUtil;
import jp.ecuacion.splib.web.adminlogin.AdminLoginTestSupport.MutableClock;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;

/**
 * Tests the requests checked by the session check (A-21 of {@code inquiry}).
 */
@DisplayName("AdminSessionCheckFilter")
class AdminSessionCheckFilterTest {

  private static final String ADMIN = "admin@example.com";

  private final Map<String, SplibAdminAccountState> accounts = new HashMap<>();
  private final SplibAdminLoginProtection protection = new SplibAdminLoginProtection(
      new MockEnvironment()
          .withProperty(SplibAdminLoginProtection.PROPERTY_SESSION_CHECK_ENABLED, "true"),
      accounts::get, new CapturingMailUtil(), "/admin/home/page", new MutableClock());
  private final MockHttpSession session = new MockHttpSession();
  private final Authentication admin = UsernamePasswordAuthenticationToken.authenticated(ADMIN,
      null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN_FULL_ACCESS")));

  @BeforeEach
  void setUp() throws Exception {
    accounts.put(ADMIN, new SplibAdminAccountState(ADMIN, "stamp1"));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    protection.createLoginSuccessHandler().onAuthenticationSuccess(request,
        new MockHttpServletResponse(), admin);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Returns the response, or {@code null} when the request reached the next filter. */
  private @Nullable MockHttpServletResponse doFilter(Authentication authentication, String uri)
      throws Exception {
    SecurityContextHolder.getContext().setAuthentication(authentication);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setServletPath(uri);
    request.setSession(session);
    MockHttpServletResponse response = new MockHttpServletResponse();
    boolean[] reached = {false};
    FilterChain chain = (req, res) -> reached[0] = true;
    protection.createSessionCheckFilter().doFilter(request, response, chain);
    return reached[0] ? null : response;
  }

  private Authentication switchedUser() {
    return UsernamePasswordAuthenticationToken.authenticated("123", null,
        List.of(new SimpleGrantedAuthority("ROLE_GENERAL"),
            new SwitchUserGrantedAuthority("ROLE_PREVIOUS_ADMINISTRATOR", admin)));
  }

  @Test
  @DisplayName("passes /admin/** while the account has not changed")
  void valid() throws Exception {
    assertThat(doFilter(admin, "/admin/home/page")).isNull();
  }

  @Test
  @DisplayName("logs out on /admin/** when the stamp has changed")
  void stampChanged() throws Exception {
    accounts.put(ADMIN, new SplibAdminAccountState(ADMIN, "stamp2"));
    MockHttpServletResponse response =
        Objects.requireNonNull(doFilter(admin, "/admin/home/page"));

    assertThat(response.getRedirectedUrl())
        .isEqualTo(SplibAdminLoginProtection.ADMIN_LOGIN_PAGE_PATH);
    assertThat(session.isInvalid()).isTrue();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("checks the admin who started the switch user, on any path")
  void switchedUserChecked() throws Exception {
    assertThat(doFilter(switchedUser(), "/account/home/page")).isNull();

    accounts.remove(ADMIN);
    MockHttpServletResponse response =
        Objects.requireNonNull(doFilter(switchedUser(), "/account/home/page"));
    assertThat(response.getRedirectedUrl())
        .isEqualTo(SplibAdminLoginProtection.ADMIN_LOGIN_PAGE_PATH);
  }

  @Test
  @DisplayName("does not check other paths of users who are not switched")
  void otherPaths() throws Exception {
    accounts.remove(ADMIN);
    Authentication general = UsernamePasswordAuthenticationToken.authenticated("user", null,
        List.of(new SimpleGrantedAuthority("ROLE_GENERAL")));
    assertThat(doFilter(general, "/account/home/page")).isNull();
    assertThat(doFilter(admin, "/public/adminLogin/page")).isNull();
  }
}
