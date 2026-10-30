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
package jp.ecuacion.splib.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SplibWebSecurityConfigForSwitchUser")
class SplibWebSecurityConfigForSwitchUserTest {

  private static final Authentication AUTH =
      UsernamePasswordAuthenticationToken.authenticated("user", null, List.of());

  /** Implements the methods with no arguments only, as the existing apps do. */
  private static class FixedConfig extends SplibWebSecurityConfigForSwitchUser {

    String doneUrl = "/account/home/page";

    FixedConfig() {
      super(mock(UserDetailsService.class));
    }

    @Override
    protected String getSwitchingUserDonePagePath() {
      return doneUrl;
    }

    @Override
    protected String getSwitchingUserFailurePagePath() {
      return "/admin/failure/page";
    }

    @Override
    protected String getExitingUserDonePagePath() {
      return "/admin/home/page";
    }
  }

  /** Decides the paths by the request and the session. */
  private static class RequestAwareConfig extends FixedConfig {

    final List<String> calls = new ArrayList<>();

    @Override
    protected String getSwitchingUserDonePagePath(HttpServletRequest request) {
      calls.add("switchPath");
      String id = request.getParameter("id");
      if (id == null) {
        return super.getSwitchingUserDonePagePath(request);
      }

      request.getSession().setAttribute("returnPath", "/admin/detail/page?id=" + id);
      return "/account/detail/page?id=" + id;
    }

    @Override
    protected String getSwitchingUserFailurePagePath(HttpServletRequest request) {
      return "/admin/detail/page?id=" + request.getParameter("id");
    }

    @Override
    protected String getExitingUserDonePagePath(HttpServletRequest request) {
      calls.add("exitPath");
      Object path = request.getSession().getAttribute("returnPath");
      return path == null ? super.getExitingUserDonePagePath(request) : (String) path;
    }

    @Override
    protected void onSwitchingUserDone(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) {
      calls.add("onSwitch");
    }

    @Override
    protected void onExitingUserDone(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) {
      calls.add("onExit");
      request.getSession().removeAttribute("returnPath");
    }
  }

  private static MockHttpServletRequest request(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app" + uri);
    request.setContextPath("/app");
    return request;
  }

  private static @Nullable String success(SplibWebSecurityConfigForSwitchUser config,
      MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    successHandler(config).onAuthenticationSuccess(request, response, AUTH);
    return response.getRedirectedUrl();
  }

  private static @Nullable String failure(SplibWebSecurityConfigForSwitchUser config,
      MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    failureHandler(config).onAuthenticationFailure(request, response,
        new BadCredentialsException("failed"));
    return response.getRedirectedUrl();
  }

  private static AuthenticationSuccessHandler successHandler(
      SplibWebSecurityConfigForSwitchUser config) {
    SwitchUserFilter filter = config.switchUserFilter();
    return (AuthenticationSuccessHandler) Objects
        .requireNonNull(ReflectionTestUtils.getField(filter, "successHandler"));
  }

  private static AuthenticationFailureHandler failureHandler(
      SplibWebSecurityConfigForSwitchUser config) {
    SwitchUserFilter filter = config.switchUserFilter();
    return (AuthenticationFailureHandler) Objects
        .requireNonNull(ReflectionTestUtils.getField(filter, "failureHandler"));
  }

  @Nested
  @DisplayName("with the methods with no arguments only")
  class Fixed {

    @Test
    @DisplayName("redirects to the fixed paths")
    void redirectsToFixedPaths() throws Exception {
      FixedConfig config = new FixedConfig();

      assertThat(success(config, request("/admin/switchUser"))).isEqualTo("/app/account/home/page");
      assertThat(success(config, request("/account/exitUser"))).isEqualTo("/app/admin/home/page");
      assertThat(failure(config, request("/admin/switchUser")))
          .isEqualTo("/app/admin/failure/page");
    }
  }

  @Nested
  @DisplayName("with the methods with a request argument")
  class RequestAware {

    @Test
    @DisplayName("decides the paths by the request, and the exit path by the session")
    void decidesPathsByRequest() throws Exception {
      RequestAwareConfig config = new RequestAwareConfig();
      MockHttpServletRequest switchRequest = request("/admin/switchUser");
      switchRequest.setParameter("id", "12");

      assertThat(success(config, switchRequest)).isEqualTo("/app/account/detail/page?id=12");

      MockHttpServletRequest exitRequest = request("/account/exitUser");
      exitRequest.setSession(switchRequest.getSession());
      assertThat(success(config, exitRequest)).isEqualTo("/app/admin/detail/page?id=12");
      assertThat(exitRequest.getSession().getAttribute("returnPath")).isNull();

      assertThat(config.calls).containsExactly("switchPath", "onSwitch", "exitPath", "onExit");
    }

    @Test
    @DisplayName("calls the methods on every request")
    void callsOnEveryRequest() throws Exception {
      RequestAwareConfig config = new RequestAwareConfig();

      assertThat(success(config, request("/admin/switchUser"))).isEqualTo("/app/account/home/page");
      config.doneUrl = "/account/other/page";
      assertThat(success(config, request("/admin/switchUser")))
          .isEqualTo("/app/account/other/page");

      MockHttpServletRequest failureRequest = request("/admin/switchUser");
      failureRequest.setParameter("id", "3");
      assertThat(failure(config, failureRequest)).isEqualTo("/app/admin/detail/page?id=3");
    }
  }

  @Nested
  @DisplayName("with a path outside the app")
  class OutsidePath {

    @Test
    @DisplayName("rejects the path")
    void rejects() {
      FixedConfig config = new FixedConfig();
      for (String path : new String[] {"//example.com", "/\\example.com", "https://example.com",
          "example"}) {
        config.doneUrl = path;
        assertThatThrownBy(() -> success(config, request("/admin/switchUser")))
            .isInstanceOf(IllegalStateException.class);
      }
    }
  }
}
