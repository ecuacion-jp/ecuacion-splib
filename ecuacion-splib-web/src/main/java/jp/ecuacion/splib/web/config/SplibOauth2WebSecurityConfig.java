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

import jp.ecuacion.splib.core.config.SplibHttpClientConfig;
import jp.ecuacion.splib.web.oauth2.SplibAppleClientSecretService;
import jp.ecuacion.splib.web.oauth2.SplibOauth2AuthFailureHandler;
import jp.ecuacion.splib.web.oauth2.SplibOauth2AuthSuccessHandler;
import jp.ecuacion.splib.web.oauth2.SplibOauth2AuthorizationRequestResolver;
import jp.ecuacion.splib.web.oauth2.SplibOauth2Config;
import jp.ecuacion.splib.web.oauth2.SplibOauth2HttpClients;
import jp.ecuacion.splib.web.oauth2.SplibOauth2TokenRequestParametersConverter;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;

/**
 * Provides the abstract SecurityConfig class for applications that add
 * Google / Apple OAuth2 (SSO) login on top of the regular id/password login.
 *
 * <p>Extend this instead of {@link SplibWebSecurityConfig} to enable social login. The
 *     required OAuth2 client registrations are configured via standard Spring Security
 *     properties ({@code spring.security.oauth2.client.*}), and a {@code SplibOauth2UserHandler}
 *     bean must be registered in the application context.</p>
 *
 * <p>The other beans needed for OAuth2 login ({@code SplibOauth2AuthSuccessHandler},
 *     {@code SplibOauth2RegistrationConfirmService} and {@code SplibAppleClientSecretService})
 *     are registered by {@link SplibOauth2Config} imported by this class,
 *     so the app doesn't need to scan {@code jp.ecuacion.splib.web.oauth2}.</p>
 *
 * <p>Since this class references OAuth2 client types, {@code spring-boot-starter-oauth2-client}
 *     must be on the app's own classpath (it is a {@code provided} dependency of
 *     {@code ecuacion-splib-web}, so apps that don't extend this class don't carry it).</p>
 */
@Import(SplibOauth2Config.class)
public abstract class SplibOauth2WebSecurityConfig extends SplibWebSecurityConfig {

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

    // Adds response_mode=form_post for Apple, which is required by Apple
    // when the name or email scope is requested.
    SplibOauth2AuthorizationRequestResolver requestResolver =
        new SplibOauth2AuthorizationRequestResolver(
            clientRegistrationRepository, "/oauth2/authorization");

    oauth2SuccessHandler.setDefaultTargetUrl(getDefaultSuccessUrl());

    // Obtained here to keep the constructor, which the apps' subclasses call, unchanged.
    ClientHttpRequestFactory requestFactory =
        http.getSharedObject(ApplicationContext.class).getBean(
            SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY, ClientHttpRequestFactory.class);

    // splib uses OAuth2 only for login and doesn't use the tokens afterwards,
    // so they are kept in the session instead of Spring Boot's default
    // (InMemoryOAuth2AuthorizedClientService, which keeps them until the app stops).
    // Apps that call the provider's APIs from batches should store the tokens on their own.
    http.oauth2Login(oauth2 -> oauth2.loginPage(getLoginNeededPage())
        .authorizedClientRepository(new HttpSessionOAuth2AuthorizedClientRepository())
        .authorizationEndpoint(ep -> ep.authorizationRequestResolver(requestResolver))
        .tokenEndpoint(ep -> ep.accessTokenResponseClient(buildTokenResponseClient(requestFactory)))
        .successHandler(oauth2SuccessHandler)
        .failureHandler(new SplibOauth2AuthFailureHandler(getLoginNeededPage())));
  }

  /**
   * Builds a token-response client that replaces {@code client_secret} with Apple's JWT
   * when a token request is sent to Apple.
   *
   * @param requestFactory requestFactory
   */
  private RestClientAuthorizationCodeTokenResponseClient buildTokenResponseClient(
      ClientHttpRequestFactory requestFactory) {
    RestClientAuthorizationCodeTokenResponseClient client =
        new RestClientAuthorizationCodeTokenResponseClient();
    client.setRestClient(SplibOauth2HttpClients.forTokenEndpoint(requestFactory));

    // setParametersConverter (not addParametersConverter, which adds a second client_secret)
    SplibOauth2TokenRequestParametersConverter converter =
        new SplibOauth2TokenRequestParametersConverter(appleClientSecretService);
    client.setParametersConverter(converter::convert);

    return client;
  }
}
