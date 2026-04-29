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
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import jp.ecuacion.splib.web.oauth2.SplibAppleClientSecretService;
import jp.ecuacion.splib.web.oauth2.SplibOauth2AuthSuccessHandler;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Provides the abstract SecurityConfig class.
 *
 * <p>Since using this class is not mandatory in the library,
 *     it's abstract and It has no annotations to be recognized as it.
 *     If you want to use this, create a new class
 *     which extends it and put class annotations on the new class:
 *     {@code Configuration} and {@code EnableWebSecurity}.</p>
 *
 * <p>To enable Google / Apple social login, register a
 *     {@code SplibOauth2UserHandler} bean in your application context.
 *     The required OAuth2 client registrations are then configured via
 *     standard Spring Security properties
 *     ({@code spring.security.oauth2.client.*}).</p>
 */
public abstract class SplibWebSecurityConfig {

  /**
   * Defines the string for the role "ACCOUNT_FULL_ACCESS".
   */
  public static final String ACCOUNT_FULL_ACCESS = "ACCOUNT_FULL_ACCESS";

  /** Attribute key used by Spring Security to store the registrationId in the builder. */
  private static final String REGISTRATION_ID_ATTR =
      OAuth2AuthorizationRequest.class.getName() + ".REGISTRATION_ID";

  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Autowired(required = false)
  @Nullable
  private SplibOauth2AuthSuccessHandler oauth2SuccessHandler;

  @Autowired(required = false)
  @Nullable
  private SplibAppleClientSecretService appleClientSecretService;

  @Autowired(required = false)
  @Nullable
  private ClientRegistrationRepository clientRegistrationRepository;

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
  protected abstract List<AuthorizationBean> getRoleInfo();

  /**
   * Returns the authority list of {@code AuthorizationBean}.
   *
   * @return the authority list of AuthorizationBean
   */
  protected abstract List<AuthorizationBean> getAuthorityInfo();

  @Bean
  PasswordEncoder passwordEncoder() {
    return passwordEncoder;
  }

  /**
   * Adds security settings to the {@code HttpSecurity} object.
   */
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.httpBasic(basic -> basic.disable());

    http.formLogin(login -> login.loginPage(getLoginNeededPage())
        .loginProcessingUrl("/public/login/action").usernameParameter("login.username")
        .passwordParameter("login.password").defaultSuccessUrl(getDefaultSuccessUrl(), true)
        .failureUrl("/public/login/page?error"));

    if (oauth2SuccessHandler != null && clientRegistrationRepository != null) {
      configureOauth2Login(http);
    }

    http.authorizeHttpRequests(
        requests -> requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
            .permitAll().requestMatchers("/public/**").permitAll()
            .requestMatchers("/ecuacion/public/**").permitAll()
            // Used when impersonated users exit
            .requestMatchers("/account/exitUser").permitAll());

    // Reserved role: ACCOUNT_FULL_ACCESS can be used if you want an account to have the open
    // permission to all page for like group administrator.
    List<AuthorizationBean> roleList =
        getRoleInfo() == null ? new ArrayList<>() : new ArrayList<>(getRoleInfo());
    roleList.add(new AuthorizationBean("/account/**", ACCOUNT_FULL_ACCESS));
    for (AuthorizationBean bean : roleList) {

      // ACCOUNT_FULL_ACCESS needs to be added to Authorization settings for each page to keep the
      // permission to access the page.
      // It might be a each app's task but this is an complecated functions so the permission for
      // ACCOUNT_FULL_ACCESS is automatically granted here.
      http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
          .hasAnyRole(bean.addAndGetRolesOrAuthorities(ACCOUNT_FULL_ACCESS)));
    }

    if (getAuthorityInfo() != null) {
      for (AuthorizationBean bean : getAuthorityInfo()) {
        http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
            .hasAnyAuthority(bean.getRolesOrAuthorities()));
      }
    }

    http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());

    http.logout(logout -> logout.logoutUrl("/public/logout")
        .logoutSuccessUrl("/public/login/page?logoutDone"));

    http.exceptionHandling(handling -> handling.accessDeniedPage(getAccessDeniedPage()));

    return http.build();
  }

  /**
   * Configures oauth2Login. Called only when a {@code SplibOauth2UserHandler} bean is present.
   */
  private void configureOauth2Login(HttpSecurity http) throws Exception {

    // Apple sends the authorization code as a POST (response_mode=form_post).
    // Exempt OAuth2 redirect endpoints from CSRF so these POST callbacks are not blocked.
    http.csrf(csrf -> csrf.ignoringRequestMatchers("/login/oauth2/code/*"));

    DefaultOAuth2AuthorizationRequestResolver requestResolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, "/oauth2/authorization");

    // Add response_mode=form_post for Apple so the user's name is returned on first login.
    requestResolver.setAuthorizationRequestCustomizer(builder -> {
      String[] registrationIdHolder = {null};
      builder.attributes(attrs ->
          registrationIdHolder[0] = (String) attrs.get(REGISTRATION_ID_ATTR));
      if ("apple".equals(registrationIdHolder[0])) {
        builder.additionalParameters(params -> params.put("response_mode", "form_post"));
      }
    });

    SplibOauth2AuthSuccessHandler handler = ObjectsUtil.requireNonNull(oauth2SuccessHandler);
    handler.setDefaultTargetUrl(getDefaultSuccessUrl());

    http.oauth2Login(oauth2 -> oauth2
        .loginPage(getLoginNeededPage())
        .authorizationEndpoint(ep -> ep.authorizationRequestResolver(requestResolver))
        .tokenEndpoint(ep -> ep.accessTokenResponseClient(buildTokenResponseClient()))
        .successHandler(handler));
  }

  /**
   * Builds a token-response client that injects Apple's JWT client secret
   * when a token request targets the "apple" registration.
   */
  private DefaultAuthorizationCodeTokenResponseClient buildTokenResponseClient() {
    DefaultAuthorizationCodeTokenResponseClient client =
        new DefaultAuthorizationCodeTokenResponseClient();

    SplibAppleClientSecretService appleService = appleClientSecretService;
    if (appleService != null) {
      OAuth2AuthorizationCodeGrantRequestEntityConverter converter =
          new OAuth2AuthorizationCodeGrantRequestEntityConverter();
      converter.addParametersConverter(grantRequest -> {
        if (!"apple".equals(grantRequest.getClientRegistration().getRegistrationId())) {
          return null;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_secret", appleService.generateClientSecret());
        return params;
      });
      client.setRequestEntityConverter(converter);
    }

    return client;
  }
}
