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

import jp.ecuacion.splib.web.config.SplibOauth2WebSecurityConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
 */
@Configuration(value = "splibOauth2Config", proxyBeanMethods = false)
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
}
