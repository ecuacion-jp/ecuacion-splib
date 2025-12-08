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
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 *  Provides the abstract SecurityConfig class for admin.
 * 
 * <p>Since using this class is not mandatory in the library, 
 *     it's abstract and It has no annotations to be recognized as it.
 *     If you want to use this, create a new class 
 *     which extends it and put class annotations on the new class: 
 *     {@code Configuration} and {@code EnableWebSecurity}.</p>
 */
public abstract class SplibWebSecurityConfigForAdmin {

  /**
   * Defines the string for the role "ADMIN_FULL_ACCESS".
   */
  public static final String ADMIN_FULL_ACCESS = "ADMIN_FULL_ACCESS";

  /**
   * Returns the url when the login procedure successfully ended.
   */
  protected abstract String getDefaultSuccessUrl();

  /**
   * Returns the url when the login needed page when there is no logged in account in the session.
   */
  protected abstract String getLoginNeededPage();

  /**
   * Returns the role list of {@code AuthorizationBean}.
   * 
   * <p>There's a reserved role: {@code ACCOUNT_FULL_ACCESS}. 
   *     This offers full access to /account/** 
   *     so it's easily used for admin user or power user.</p>
   *     
   * @return the role list of AuthorizationBean
   */
  protected abstract List<AuthorizationBean> getRoleInfo();

  /**
   * Returns the authority list of {@code AuthorizationBean}.
   * 
   * @return the authority list of AuthorizationBean
   */
  protected abstract List<AuthorizationBean> getAuthorityInfo();

  /**
   * Returns the url when the access denied page is accessed.
   * 
   * <p>This happens in the case of non-exist url access and csrf token error.</p>
   */
  protected abstract String getAccessDeniedPage();

  @Autowired
  UserDetailsService userDetailsService;

  /**
   * Adds security settings to the {@code HttpSecurity} object. 
   * 
   * <p>Since {@code @Order(11)} is added to the method, 
   *     the priority is higher than {@code SplibWebSecurityConfig#filterChain},
   *     and lower than {@code jp.ecuacion.splib.rest.config.SplibRestSecurityConfig}.</p>
   */
  @Order(11)
  @Bean
  SecurityFilterChain filterChainForAdmin(HttpSecurity http) throws Exception {

    http.securityMatcher("/public/admin*/**", "/admin/**");

    http.httpBasic(basic -> basic.disable());

    http.formLogin(login -> login.loginPage(getLoginNeededPage())
        .loginProcessingUrl("/public/adminLogin/action").usernameParameter("adminLogin.username")
        .passwordParameter("adminLogin.password").defaultSuccessUrl(getDefaultSuccessUrl(), true)
        .failureUrl("/public/adminLogin/page?error"));

    // for impersonate login
    http.authorizeHttpRequests(
        requests -> requests.requestMatchers("/admin/switchUser").hasRole(ADMIN_FULL_ACCESS));

    http.authorizeHttpRequests(
        requests -> requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
            .permitAll().requestMatchers("/public/admin*/**").permitAll());

    // Reserved role: ADMIN_FULL_ACCESS can be used if you want an account to have the open
    // permission to all page for like group administrator.
    List<AuthorizationBean> roleList = getRoleInfo() == null ? new ArrayList<>() : getRoleInfo();
    roleList.add(new AuthorizationBean("/admin/**", ADMIN_FULL_ACCESS));
    for (AuthorizationBean bean : roleList) {

      // ADMIN_FULL_ACCESS needs to be added to Authorization settings for each page to keep the
      // permission to access the page.
      // It might be a each app's task but this is an complecated functions so the permission for
      // ADMIN_FULL_ACCESS is automatically granted here.
      http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
          .hasAnyRole(bean.addAndGetRolesOrAuthorities(ADMIN_FULL_ACCESS)));
    }

    if (getAuthorityInfo() != null) {
      for (AuthorizationBean bean : getAuthorityInfo()) {
        http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
            .hasAnyAuthority(bean.getRolesOrAuthorities()));
      }
    }

    http.logout(logout -> logout.logoutUrl("/public/adminLogout")
        .logoutSuccessUrl("/public/adminLogin/page?logoutDone"));

    http.exceptionHandling(handling -> handling.accessDeniedPage(getAccessDeniedPage()));

    return http.build();
  }

  /**
   *  for impersonate login.
   */
  @Bean
  @ConditionalOnProperty(name = "jp.ecuacion.splib.web.switch-user.enabled", havingValue = "true",
      matchIfMissing = false)
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
    filter.setSuccessHandler(new CustomLoginSuccessHandler());
    // filter.setSwitchFailureUrl("/public/error");
    filter.setFailureHandler(new CustomLoginFailureHandler());

    return filter;
  }

  private static class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
        final HttpServletResponse response, final Authentication authentication)
        throws IOException, ServletException {

      if (request.getRequestURI().endsWith("/admin/switchUser")) {
        response.sendRedirect(
            PropertyFileUtil.getApplication("jp.ecuacion.splib.web.switch-user.switch-url"));

      } else {
        response.sendRedirect(
            PropertyFileUtil.getApplication("jp.ecuacion.splib.web.switch-user.exit-url"));
      }
    }
  }

  private static class CustomLoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception) throws IOException, ServletException {
      response.sendRedirect("jp.ecuacion.splib.web.switch-user.exit-url");
    }
  }
}
