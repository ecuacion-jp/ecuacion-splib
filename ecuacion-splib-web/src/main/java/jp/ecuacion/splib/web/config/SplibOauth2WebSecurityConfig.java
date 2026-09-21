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

import jp.ecuacion.splib.web.oauth2.SplibAppleClientSecretService;
import jp.ecuacion.splib.web.oauth2.SplibOauth2AuthSuccessHandler;
import org.jspecify.annotations.Nullable;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Provides the abstract SecurityConfig class for applications that add
 * Google / Apple OAuth2 (SSO) login on top of the regular id/password login.
 *
 * <p>Extend this instead of {@link SplibWebSecurityConfig} to enable social login. The
 *     required OAuth2 client registrations are configured via standard Spring Security
 *     properties ({@code spring.security.oauth2.client.*}), and a {@code SplibOauth2UserHandler}
 *     bean must be registered in the application context.</p>
 *
 * <p>Since this class references OAuth2 client types, {@code spring-boot-starter-oauth2-client}
 *     must be on the app's own classpath (it is a {@code provided} dependency of
 *     {@code ecuacion-splib-web}, so apps that don't extend this class don't carry it).</p>
 */
public abstract class SplibOauth2WebSecurityConfig extends SplibWebSecurityConfig {

  /** Attribute key used by Spring Security to store the registrationId in the builder. */
  private static final String REGISTRATION_ID_ATTR =
      OAuth2AuthorizationRequest.class.getName() + ".REGISTRATION_ID";

  private final SplibOauth2AuthSuccessHandler oauth2SuccessHandler;

  @Nullable
  private final SplibAppleClientSecretService appleClientSecretService;

  private final ClientRegistrationRepository clientRegistrationRepository;

  /**
   * Constructs a new instance.
   *
   * @param oauth2SuccessHandler oauth2SuccessHandler
   * @param appleClientSecretService appleClientSecretService, may be {@code null} when Apple
   *     login is not configured
   * @param clientRegistrationRepository clientRegistrationRepository
   */
  protected SplibOauth2WebSecurityConfig(SplibOauth2AuthSuccessHandler oauth2SuccessHandler,
      @Nullable SplibAppleClientSecretService appleClientSecretService,
      ClientRegistrationRepository clientRegistrationRepository) {
    this.oauth2SuccessHandler = oauth2SuccessHandler;
    this.appleClientSecretService = appleClientSecretService;
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  @Override
  protected void configureOauth2Login(HttpSecurity http) throws Exception {

    // Apple sends the authorization code as a POST (response_mode=form_post).
    // Exempt OAuth2 redirect endpoints from CSRF so these POST callbacks are not blocked.
    http.csrf(csrf -> csrf.ignoringRequestMatchers("/login/oauth2/code/*"));

    DefaultOAuth2AuthorizationRequestResolver requestResolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, "/oauth2/authorization");

    // Add response_mode=form_post for Apple so the user's name is returned on first login.
    requestResolver.setAuthorizationRequestCustomizer(builder -> {
      String[] registrationIdHolder = {null};
      builder
          .attributes(attrs -> registrationIdHolder[0] = (String) attrs.get(REGISTRATION_ID_ATTR));
      if ("apple".equals(registrationIdHolder[0])) {
        builder.additionalParameters(params -> params.put("response_mode", "form_post"));
      }
    });

    oauth2SuccessHandler.setDefaultTargetUrl(getDefaultSuccessUrl());

    http.oauth2Login(oauth2 -> oauth2.loginPage(getLoginNeededPage())
        .authorizationEndpoint(ep -> ep.authorizationRequestResolver(requestResolver))
        .tokenEndpoint(ep -> ep.accessTokenResponseClient(buildTokenResponseClient()))
        .successHandler(oauth2SuccessHandler));
  }

  /**
   * Builds a token-response client that injects Apple's JWT client secret
   * when a token request targets the "apple" registration.
   */
  private RestClientAuthorizationCodeTokenResponseClient buildTokenResponseClient() {
    RestClientAuthorizationCodeTokenResponseClient client =
        new RestClientAuthorizationCodeTokenResponseClient();

    SplibAppleClientSecretService appleService = appleClientSecretService;
    if (appleService != null) {
      client.addParametersConverter(grantRequest -> {
        if (!"apple".equals(grantRequest.getClientRegistration().getRegistrationId())) {
          return new LinkedMultiValueMap<>();
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_secret", appleService.generateClientSecret());
        return params;
      });
    }

    return client;
  }
}
