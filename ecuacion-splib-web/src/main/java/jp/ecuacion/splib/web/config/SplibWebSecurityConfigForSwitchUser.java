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
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 *  Provides the abstract SecurityConfig class for swith user (impersonation).
 * 
 * <p>Since using this class is not mandatory in the library, 
 *     it's abstract and It has no annotations to be recognized as it.
 *     If you want to use this, create a new class 
 *     which extends it and put class annotations on the new class: 
 *     {@code Configuration}.</p>
 *
 * <p>The pages to redirect to are fixed per app by the methods with no arguments.
 *     To decide them per request (for example, by the page the switching is started from),
 *     override the methods with an {@code HttpServletRequest} argument.
 *     Since the request to exit ({@code POST /account/exitUser}) has no information
 *     about where the switching is started from, keep it in the session when switching
 *     (in {@link #onSwitchingUserDone(HttpServletRequest, HttpServletResponse, Authentication)}
 *     or before posting to {@code /admin/switchUser}), and read it when exiting.
 *     The session is kept through switching and exiting.</p>
 */
public abstract class SplibWebSecurityConfigForSwitchUser {

  private static final RequestMatcher SWITCH_USER_MATCHER =
      PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/admin/switchUser");

  private static final RequestMatcher EXIT_USER_MATCHER =
      PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/account/exitUser");

  /**
   * Returns the path of the page displayed when switching user is successfully done.
   * 
   * @return url path string, like '/account/dashboard/page'.
   */
  protected abstract String getSwitchingUserDonePagePath();

  /**
   * Returns the path of the page displayed when switching user is successfully done,
   *     decided by the request.
   *
   * <p>Called on every successful switching.
   *     By default it returns {@link #getSwitchingUserDonePagePath()}.</p>
   *
   * @param request request to switch user
   * @return url path string in the app, like '/account/dashboard/page'.
   *     It must start with '/' and must not start with '//' or '/\'.
   */
  protected String getSwitchingUserDonePagePath(HttpServletRequest request) {
    return getSwitchingUserDonePagePath();
  }

  /**
   * Returns the path of the page displayed when switching user is failed.
   * 
   * @return url path string, like '/admin/dashboard/page'.
   */
  protected abstract String getSwitchingUserFailurePagePath();

  /**
   * Returns the path of the page displayed when switching user is failed,
   *     decided by the request.
   *
   * <p>Called on every failed switching.
   *     By default it returns {@link #getSwitchingUserFailurePagePath()}.</p>
   *
   * @param request request to switch user
   * @return url path string in the app, like '/admin/dashboard/page'.
   *     It must start with '/' and must not start with '//' or '/\'.
   */
  protected String getSwitchingUserFailurePagePath(HttpServletRequest request) {
    return getSwitchingUserFailurePagePath();
  }

  /**
   * Returns the path of the page displayed when exitting the switched user is successfully done.
   * 
   * @return url path string, like '/admin/dashboard/page'.
   */
  protected abstract String getExitingUserDonePagePath();

  /**
   * Returns the path of the page displayed when exitting the switched user is successfully done,
   *     decided by the request.
   *
   * <p>Called on every successful exiting.
   *     By default it returns {@link #getExitingUserDonePagePath()}.</p>
   *
   * @param request request to exit the switched user.
   *     Its session is the one used when switching.
   * @return url path string in the app, like '/admin/dashboard/page'.
   *     It must start with '/' and must not start with '//' or '/\'.
   */
  protected String getExitingUserDonePagePath(HttpServletRequest request) {
    return getExitingUserDonePagePath();
  }

  /**
   * Is called when switching user is successfully done, before the redirect.
   *
   * <p>It's called after {@link #getSwitchingUserDonePagePath(HttpServletRequest)},
   *     so the information used to decide the path can be removed here.
   *     Does nothing by default.</p>
   *
   * @param request request to switch user
   * @param response response
   * @param authentication authentication of the switched user
   */
  protected void onSwitchingUserDone(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {
    // does nothing by default
  }

  /**
   * Is called when exitting the switched user is successfully done, before the redirect.
   *
   * <p>It's called after {@link #getExitingUserDonePagePath(HttpServletRequest)},
   *     so the information used to decide the path can be removed here.
   *     Use it also to remove the information kept in the session while switched.
   *     Does nothing by default.</p>
   *
   * @param request request to exit the switched user
   * @param response response
   * @param authentication authentication of the original user
   */
  protected void onExitingUserDone(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {
    // does nothing by default
  }

  UserDetailsService userDetailsService;

  /**
   * Constructs a new instance.
   *
   * @param userDetailsService userDetailsService
   */
  protected SplibWebSecurityConfigForSwitchUser(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  /**
   *  for impersonate login.
   */
  @Bean
  SwitchUserFilter switchUserFilter() {
    SwitchUserFilter filter = new SwitchUserFilter();
    // /admin/impersonateLogin/action
    filter.setUserDetailsService(userDetailsService);
    filter.setUsernameParameter("switchUser.username");
    // POST (not GET) so that switching / exiting users is CSRF-protected
    // and the target username does not appear in URLs, access logs or Referer headers.
    filter.setSwitchUserMatcher(SWITCH_USER_MATCHER);
    filter.setExitUserMatcher(EXIT_USER_MATCHER);
    filter.setSuccessHandler(new CustomLoginSuccessHandler());
    filter.setFailureHandler(new CustomLoginFailureHandler());

    return filter;
  }

  /**
   * Redirects to the path in the app.
   *
   * <p>A path like '//example.com' is rejected 
   *     since it's a url to the other site when the context path is empty.</p>
   */
  private static void redirect(HttpServletRequest request, HttpServletResponse response,
      String path) throws IOException {
    if (!path.startsWith("/") || path.startsWith("//") || path.startsWith("/\\")) {
      throw new IllegalStateException(
          "The path to redirect to must be the one in the app, starting with '/': " + path);
    }

    response.sendRedirect(request.getContextPath() + path);
  }

  private class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
        final HttpServletResponse response, final Authentication authentication)
        throws IOException, ServletException {

      if (SWITCH_USER_MATCHER.matches(request)) {
        String path = getSwitchingUserDonePagePath(request);
        onSwitchingUserDone(request, response, authentication);
        redirect(request, response, path);

      } else {
        String path = getExitingUserDonePagePath(request);
        onExitingUserDone(request, response, authentication);
        redirect(request, response, path);
      }
    }
  }

  private class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception) throws IOException, ServletException {

      redirect(request, response, getSwitchingUserFailurePagePath(request));
    }
  }
}
