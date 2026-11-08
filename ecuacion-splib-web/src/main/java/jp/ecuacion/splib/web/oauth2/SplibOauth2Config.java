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
package jp.ecuacion.splib.web.oauth2;

import jp.ecuacion.splib.core.config.SplibHttpClientConfig;
import jp.ecuacion.splib.web.config.SplibOauth2WebSecurityConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Registers the beans of this package used by {@link SplibOauth2WebSecurityConfig}.
 *
 * <p>This is imported by {@link SplibOauth2WebSecurityConfig}, so apps that extend it
 *     don't need to scan this package, and apps that don't extend it
 *     (which may not have {@code spring-boot-starter-oauth2-client}) don't get these beans.</p>
 *
 * <p>No {@code ConditionalOnBean(SplibOauth2UserHandler.class)} is used since its result depends
 *     on the order of bean registration outside auto-configurations.
 *     Apps extending {@link SplibOauth2WebSecurityConfig} must register
 *     a {@link SplibOauth2UserHandler} bean, and the startup fails if it's missing.</p>
 *
 * <p>All the outbound communication of the login uses the client of
 *     {@link SplibHttpClientConfig}, so its proxy and timeout settings apply.
 *     An app registering its own {@link JwtDecoderFactory} bean fails at startup
 *     since Spring Security requires it to be unique.</p>
 */
@Configuration(value = "splibOauth2Config", proxyBeanMethods = false)
@Import(SplibHttpClientConfig.class)
public class SplibOauth2Config {

  /**
   * Provides {@link SplibOauth2AuthSuccessHandler}.
   *
   * @param userHandler the application-provided user handler
   * @return SplibOauth2AuthSuccessHandler
   */
  @Bean
  SplibOauth2AuthSuccessHandler splibOauth2AuthSuccessHandler(
      SplibOauth2UserHandler userHandler) {
    return new SplibOauth2AuthSuccessHandler(userHandler);
  }

  /**
   * Provides {@link SplibOauth2RegistrationConfirmService}.
   *
   * @param successHandler successHandler
   * @return SplibOauth2RegistrationConfirmService
   */
  @Bean
  SplibOauth2RegistrationConfirmService splibOauth2RegistrationConfirmService(
      SplibOauth2AuthSuccessHandler successHandler) {
    return new SplibOauth2RegistrationConfirmService(successHandler);
  }

  /**
   * Provides {@link SplibAppleClientSecretService}
   *     when {@code ecuacion.oauth2.apple.team-id} is set.
   *
   * @return SplibAppleClientSecretService
   */
  @Bean
  @ConditionalOnProperty(name = "ecuacion.oauth2.apple.team-id")
  SplibAppleClientSecretService splibAppleClientSecretService() {
    return new SplibAppleClientSecretService();
  }

  /**
   * Provides the service to get user information when the {@code openid} scope isn't requested.
   *
   * @param requestFactory requestFactory
   * @return OAuth2UserService
   */
  @Bean
  OAuth2UserService<OAuth2UserRequest, OAuth2User> splibOauth2UserService(
      @Qualifier(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY)
      ClientHttpRequestFactory requestFactory) {
    return buildOauth2UserService(requestFactory);
  }

  /**
   * Provides the service to get user information when the {@code openid} scope is requested.
   *
   * <p>The login fails when the email address is not verified.
   *     See {@link SplibOauth2OidcUserService}.</p>
   *
   * @param requestFactory requestFactory
   * @return OAuth2UserService
   */
  @Bean
  OAuth2UserService<OidcUserRequest, OidcUser> splibOidcUserService(
      @Qualifier(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY)
      ClientHttpRequestFactory requestFactory) {
    OidcUserService service = new SplibOauth2OidcUserService();
    service.setOauth2UserService(buildOauth2UserService(requestFactory));
    return service;
  }

  /**
   * Provides the factory of ID token decoders.
   *
   * @param requestFactory requestFactory
   * @return JwtDecoderFactory
   */
  @Bean
  JwtDecoderFactory<ClientRegistration> splibOauth2JwtDecoderFactory(
      @Qualifier(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY)
      ClientHttpRequestFactory requestFactory) {
    return new SplibOauth2JwtDecoderFactory(new RestTemplate(requestFactory));
  }

  private DefaultOAuth2UserService buildOauth2UserService(
      ClientHttpRequestFactory requestFactory) {
    DefaultOAuth2UserService service = new DefaultOAuth2UserService();
    service.setRestOperations(SplibOauth2HttpClients.forUserInfoEndpoint(requestFactory));
    return service;
  }
}
