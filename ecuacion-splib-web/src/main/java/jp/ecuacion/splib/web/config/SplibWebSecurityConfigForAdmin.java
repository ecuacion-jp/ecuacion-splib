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


import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import jp.ecuacion.splib.web.adminlogin.SplibAdminAccountProvider;
import jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authorization.DefaultAuthorizationManagerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 *  Provides the abstract SecurityConfig class for admin.
 *
 * <p>Since using this class is not mandatory in the library,
 *     it's abstract and It has no annotations to be recognized as it.
 *     If you want to use this, create a new class
 *     which extends it and put class annotations on the new class:
 *     {@code Configuration} and {@code EnableWebSecurity}.</p>
 *
 * <p><strong>Protection of the admin login.</strong> The login code sent by mail, the check of
 *     the session of the logged-in admin, and the limit of wrong passwords can be enabled by
 *     properties. See {@link SplibAdminLoginProtection}, which is registered as a bean by this
 *     class.</p>
 */
public abstract class SplibWebSecurityConfigForAdmin {

  /**
   * The order of the filter of the session check: right after the Spring Security filters,
   *     and before {@code SwitchUserFilter} (registered with the lowest precedence).
   *
   * <p>If you change {@code spring.security.filter.order}, it must stay after it.</p>
   */
  public static final int SESSION_CHECK_FILTER_ORDER =
      SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1;

  private static final String FAILURE_URL = SplibAdminLoginProtection.ADMIN_LOGIN_PAGE_PATH
      + "?error";

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

  /**
   * Adds security settings to the {@code HttpSecurity} object.
   *
   * <p>Since {@code @Order(21)} is added to the method,
   *     the priority is higher than {@code SplibWebSecurityConfig#filterChain} 
   *     ({@code @Order(29)}),
   *     and lower than {@code jp.ecuacion.splib.rest.config.SplibRestSecurityConfig} ({@code
   *     @Order(11)}-{@code (14)}).</p>
   */
  @Order(21)
  @Bean
  SecurityFilterChain filterChainForAdmin(HttpSecurity http,
      SplibAdminLoginProtection protection) throws Exception {

    http.securityMatcher("/public/admin*/**", "/admin/**");

    http.httpBasic(basic -> basic.disable());

    if (protection.isAnyEnabled()) {
      http.formLogin(login -> login.loginPage(getLoginNeededPage())
          .loginProcessingUrl(SplibAdminLoginProtection.LOGIN_PROCESSING_PATH)
          .usernameParameter(SplibAdminLoginProtection.USERNAME_PARAMETER)
          .passwordParameter("adminLogin.password")
          .successHandler(protection.createLoginSuccessHandler())
          .failureHandler(protection.createLoginFailureHandler(FAILURE_URL)));

    } else {
      http.formLogin(login -> login.loginPage(getLoginNeededPage())
          .loginProcessingUrl(SplibAdminLoginProtection.LOGIN_PROCESSING_PATH)
          .usernameParameter(SplibAdminLoginProtection.USERNAME_PARAMETER)
          .passwordParameter("adminLogin.password").defaultSuccessUrl(getDefaultSuccessUrl(), true)
          .failureUrl(FAILURE_URL));
    }

    if (protection.isAttemptLimitEnabled()) {
      http.authenticationProvider(protection.createAttemptLimitAuthenticationProvider());
    }

    http.authorizeHttpRequests(
        requests -> requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
            .permitAll().requestMatchers("/public/admin*/**").permitAll());

    // With the login code, the pages also require FACTOR_OTT, which is added when the code is
    // entered.
    @Nullable DefaultAuthorizationManagerFactory<RequestAuthorizationContext> mfa =
        protection.isLoginCodeEnabled() ? protection.createAuthorizationManagerFactory() : null;

    // Reserved role: ADMIN_FULL_ACCESS can be used if you want an account to have the open
    // permission to all page for like group administrator.
    List<AuthorizationBean> roleList = getRoleInfo() == null ? new ArrayList<>() : getRoleInfo();
    roleList.add(new AuthorizationBean("/admin/**", ADMIN_FULL_ACCESS));
    for (AuthorizationBean bean : roleList) {

      // ADMIN_FULL_ACCESS needs to be added to Authorization settings for each page to keep the
      // permission to access the page.
      // It might be each app's task but this is a complicated function so the permission for
      // ADMIN_FULL_ACCESS is automatically granted here.
      String[] roles = bean.addAndGetRolesOrAuthorities(ADMIN_FULL_ACCESS);
      http.authorizeHttpRequests(requests -> {
        if (mfa == null) {
          requests.requestMatchers(bean.getRequestMatchers()).hasAnyRole(roles);

        } else {
          requests.requestMatchers(bean.getRequestMatchers()).access(mfa.hasAnyRole(roles));
        }
      });
    }

    if (getAuthorityInfo() != null) {
      for (AuthorizationBean bean : getAuthorityInfo()) {
        http.authorizeHttpRequests(requests -> {
          if (mfa == null) {
            requests.requestMatchers(bean.getRequestMatchers())
                .hasAnyAuthority(bean.getRolesOrAuthorities());

          } else {
            requests.requestMatchers(bean.getRequestMatchers())
                .access(mfa.hasAnyAuthority(bean.getRolesOrAuthorities()));
          }
        });
      }
    }

    http.logout(logout -> logout.logoutUrl("/public/adminLogout")
        .logoutSuccessUrl(SplibAdminLoginProtection.ADMIN_LOGIN_PAGE_PATH + "?logoutDone"));

    if (protection.isLoginCodeEnabled()) {
      http.exceptionHandling(handling -> handling
          .accessDeniedHandler(protection.createAccessDeniedHandler(getAccessDeniedPage())));

    } else {
      http.exceptionHandling(handling -> handling.accessDeniedPage(getAccessDeniedPage()));
    }

    return http.build();
  }

  /**
   * Provides the protection of the admin login. See {@link SplibAdminLoginProtection}.
   *
   * @param environment environment
   * @param accountProvider the provider of admin accounts, implemented by the app
   * @param mailUtil mailUtil
   * @return the protection
   */
  @Bean
  SplibAdminLoginProtection splibAdminLoginProtection(Environment environment,
      ObjectProvider<SplibAdminAccountProvider> accountProvider,
      ObjectProvider<SplibMailUtil> mailUtil) {
    return new SplibAdminLoginProtection(environment, accountProvider.getIfAvailable(),
        mailUtil.getIfAvailable(), getDefaultSuccessUrl());
  }

  /**
   * Registers the filter of the session check, which is enabled only when
   *     {@code jp.ecuacion.splib.web.admin-login.session-check.enabled} is {@code true}.
   *
   * @param protection protection
   * @return the registration of the filter
   */
  @Bean
  FilterRegistrationBean<@NonNull Filter> splibAdminSessionCheckFilter(
      SplibAdminLoginProtection protection) {
    FilterRegistrationBean<@NonNull Filter> registration =
        new FilterRegistrationBean<>(protection.createSessionCheckFilter());
    registration.setOrder(SESSION_CHECK_FILTER_ORDER);
    registration.setEnabled(protection.isSessionCheckEnabled());
    return registration;
  }
}
