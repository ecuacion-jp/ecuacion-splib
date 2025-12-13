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


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 *  Provides the abstract SecurityConfig class for swith user (impersonation).
 * 
 * <p>Since using this class is not mandatory in the library, 
 *     it's abstract and It has no annotations to be recognized as it.
 *     If you want to use this, create a new class 
 *     which extends it and put class annotations on the new class: 
 *     {@code Configuration}.</p>
 */
public abstract class SplibWebSecurityConfigForSwitchUser {

  /**
   * Returns the path of the page displayed when switching user is successfully done.
   * 
   * @return url path string, like '/account/dashboard/page'.
   */
  protected abstract String getSwitchingUserDonePagePath();

  /**
   * Returns the path of the page displayed when switching user is successfully done.
   * 
   * @return url path string, like '/account/dashboard/page'.
   */
  protected abstract String getSwitchingUserFailurePagePath();

  /**
   * Returns the path of the page displayed when exitting the switched user is successfully done.
   * 
   * <p>This path is also used when the switching is failed.</p>
   * 
   * @return url path string, like '/account/dashboard/page'.
   */
  protected abstract String getExitingUserDonePagePath();

  @Autowired
  UserDetailsService userDetailsService;

  /**
   *  for impersonate login.
   */
  @Bean
  SwitchUserFilter switchUserFilter() {
    SwitchUserFilter filter = new SwitchUserFilter();
    // /admin/impersonateLogin/action
    filter.setUserDetailsService(userDetailsService);
    filter.setUsernameParameter("adminLogin.username");
    // filter.setSwitchUserUrl("/admin/switchUser");
    filter.setSwitchUserMatcher(
        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/admin/switchUser"));
    // filter.setExitUserUrl("/account/exitUser");
    filter.setExitUserMatcher(
        PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/account/exitUser"));
    // filter.setTargetUrl("/account/cloudService/searchList/page");
    filter.setSuccessHandler(new CustomLoginSuccessHandler(getSwitchingUserDonePagePath(),
        getExitingUserDonePagePath()));
    // filter.setSwitchFailureUrl("/public/error");
    filter.setFailureHandler(new CustomLoginFailureHandler(getSwitchingUserFailurePagePath()));

    return filter;
  }

  private static class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private String switchingUserDonePagePath;
    private String exitingUserDonePagePath;

    public CustomLoginSuccessHandler(String switchingUserDonePagePath,
        String exitingUserDonePagePath) {
      this.switchingUserDonePagePath = switchingUserDonePagePath;
      this.exitingUserDonePagePath = exitingUserDonePagePath;
    }

    public void onAuthenticationSuccess(final HttpServletRequest request,
        final HttpServletResponse response, final Authentication authentication)
        throws IOException, ServletException {

      if (request.getRequestURI().endsWith("/admin/switchUser")) {
        response.sendRedirect(switchingUserDonePagePath);

      } else {
        response.sendRedirect(exitingUserDonePagePath);
      }
    }
  }

  private static class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    private String exitingUserDonePagePath;

    public CustomLoginFailureHandler(String exitingUserDonePagePath) {
      this.exitingUserDonePagePath = exitingUserDonePagePath;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception) throws IOException, ServletException {
      response.sendRedirect(exitingUserDonePagePath);
    }
  }
}
