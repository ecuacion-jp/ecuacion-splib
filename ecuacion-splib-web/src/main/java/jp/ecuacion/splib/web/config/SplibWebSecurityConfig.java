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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Provides the abstract SecurityConfig class.
 *
 * <p>Since using this class is not mandatory in the library,
 *     it's abstract and It has no annotations to be recognized as it.
 *     If you want to use this, create a new class
 *     which extends it and put class annotations on the new class:
 *     {@code Configuration} and {@code EnableWebSecurity}.</p>
 *
 * <p>To enable Google / Apple social login, extend
 *     {@link SplibOauth2WebSecurityConfig} instead, which adds OAuth2 login
 *     on top of this class.</p>
 */
public abstract class SplibWebSecurityConfig {

  /**
   * Defines the string for the role "ACCOUNT_FULL_ACCESS".
   */
  public static final String ACCOUNT_FULL_ACCESS = "ACCOUNT_FULL_ACCESS";

  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /**
   * Constructs a new instance.
   */
  protected SplibWebSecurityConfig() {}

  /**
   * Returns whether form login is enabled for this application.
   *
   * <p>Returns {@code true} by default.
   *     No-login applications should override this via
   *     {@link SplibWebSecurityConfigForNoLogin} which returns {@code false},
   *     causing {@code formLogin} and {@code logout} to be disabled in
   *     {@link #filterChain(HttpSecurity)}.</p>
   *
   * @return {@code true} if login is enabled
   */
  protected boolean isLoginEnabled() {
    return true;
  }

  /**
   * Returns the url when the login procedure successfully ended.
   */
  protected abstract String getDefaultSuccessUrl();

  /**
   * Returns the url when the login needed page when there is no logged in account in the session.
   *
   * <p>If this page doesn't exist,
   *     {@code org.thymeleaf.exceptions.TemplateInputException} occurs.</p>
   */
  protected abstract String getLoginNeededPage();

  /**
   * Returns the url when the access denied page is accessed.
   *
   * <p>This happens in the case of non-exist url access and csrf token error.</p>
   */
  protected abstract String getAccessDeniedPage();

  /**
   * Returns the role list of {@code AuthorizationBean}.
   *
   * <p>There's a reserved role: {@code ACCOUNT_FULL_ACCESS}.
   *     This offers full access to /account/**
   *     so it's easily used for admin user or power user.</p>
   *
   * @return the role list of AuthorizationBean
   */
  protected abstract @Nullable List<AuthorizationBean> getRoleInfo();

  /**
   * Returns the authority list of {@code AuthorizationBean}.
   *
   * @return the authority list of AuthorizationBean, or {@code null} if not used
   */
  protected abstract @Nullable List<AuthorizationBean> getAuthorityInfo();

  @Bean
  PasswordEncoder passwordEncoder() {
    return passwordEncoder;
  }

  /**
   * Adds security settings to the {@code HttpSecurity} object.
   *
   * <p>Matches {@code anyRequest()} — every path not already claimed by a more specific,
   *     lower-{@code @Order} chain ({@code SplibWebSecurityConfigForAdmin}'s {@code @Order(21)},
   *     {@code SplibBuiltinAdminSecurityConfig}'s {@code @Order(22)}, and, when
   *     {@code ecuacion-splib-rest} is also used, its {@code @Order(11)}-{@code (14)} chains for
   *     {@code /api/**}) falls through to this one, which ends in {@code anyRequest().denyAll()}
   *     — so this is effectively the application's catch-all.</p>
   *
   * <p>{@code @Order(29)} — the last number in this class's own {@code 21}-{@code 29} range (see
   *     {@code SplibRestSecurityConfig}'s javadoc for the two modules' reserved ranges) — is
   *     deliberately explicit (rather than left as the implicit {@code Ordered.LOWEST_PRECEDENCE}
   *     default) so that {@code jp.ecuacion.splib.rest.config.SplibRestSecurityConfig}'s own
   *     {@code /**} catch-all — added there specifically for apps that use
   *     {@code ecuacion-splib-rest} <em>without</em> this class, which would otherwise have no
   *     final catch-all at all — stays at a strictly lower priority than this one. That way, an
   *     app using both modules together still gets this class's real
   *     {@code permitAll}/{@code denyAll} policy for its non-{@code /api} pages, rather than an
   *     unconditional {@code denyAll} from the rest module's fallback.</p>
   */
  @Order(29)
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.httpBasic(basic -> basic.disable());

    if (isLoginEnabled()) {
      http.formLogin(login -> login.loginPage(getLoginNeededPage())
          .loginProcessingUrl("/public/login/action").usernameParameter("login.username")
          .passwordParameter("login.password").defaultSuccessUrl(getDefaultSuccessUrl(), true)
          .failureUrl("/public/login/page?error"));

      configureOauth2Login(http);
    } else {
      http.formLogin(login -> login.disable());
    }

    http.authorizeHttpRequests(
        requests -> requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
            .permitAll().requestMatchers("/public/**").permitAll()
            .requestMatchers("/ecuacion-splib/public/**").permitAll()
            // Used when impersonated users exit
            .requestMatchers("/account/exitUser").permitAll());

    // Reserved role: ACCOUNT_FULL_ACCESS can be used if you want an account to have the open
    // permission to all page for like group administrator.
    List<AuthorizationBean> roleList = getRoleInfo() == null ? new ArrayList<>()
        : new ArrayList<>(
            getRoleInfo() == null ? new ArrayList<>() : Objects.requireNonNull(getRoleInfo()));
    roleList.add(new AuthorizationBean("/account/**", ACCOUNT_FULL_ACCESS));
    for (AuthorizationBean bean : roleList) {

      // ACCOUNT_FULL_ACCESS needs to be added to Authorization settings for each page to keep the
      // permission to access the page.
      // It might be each app's task but this is a complicated function so the permission for
      // ACCOUNT_FULL_ACCESS is automatically granted here.
      http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
          .hasAnyRole(bean.addAndGetRolesOrAuthorities(ACCOUNT_FULL_ACCESS)));
    }

    if (getAuthorityInfo() != null) {
      for (AuthorizationBean bean : Objects.requireNonNull(getAuthorityInfo())) {
        http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
            .hasAnyAuthority(bean.getRolesOrAuthorities()));
      }
    }

    http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());

    if (isLoginEnabled()) {
      http.logout(logout -> logout.logoutUrl("/public/logout")
          .logoutSuccessUrl("/public/login/page?logoutDone"));
    }

    http.exceptionHandling(
        handling -> handling.accessDeniedPage(getAccessDeniedPage()).authenticationEntryPoint(
            (request, response, authException) -> response.sendRedirect(getAccessDeniedPage())));

    return http.build();
  }

  /**
   * Configures oauth2Login. No-op by default; overridden by
   * {@link SplibOauth2WebSecurityConfig} for applications that use Google / Apple SSO.
   */
  protected void configureOauth2Login(HttpSecurity http) throws Exception {}
}
