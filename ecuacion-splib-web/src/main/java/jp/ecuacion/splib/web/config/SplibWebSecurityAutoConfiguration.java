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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auto-configures a permissive {@link SecurityFilterChain} fallback
 * when no {@link SplibWebSecurityConfig} subclass bean is present in the application context.
 *
 * <p><strong>Intended use.</strong> This auto-configuration exists so that applications
 *     depending on {@code ecuacion-splib-web} for non-web concerns
 *     (e.g. only Jakarta Validation utilities) can start up
 *     without having to define a {@code SecurityConfig}.
 *     It is a fallback for such limited-use cases,
 *     not a general-purpose security configuration for web applications.</p>
 *
 * <p><strong>Web applications must extend {@link SplibWebSecurityConfig}.</strong>
 *     {@link SplibWebSecurityConfig} provides the form login URLs, OAuth2 login wiring,
 *     role/authority-based authorization
 *     (including the reserved {@code ACCOUNT_FULL_ACCESS} role for {@code /account/**}),
 *     and other conventions that the rest of {@code ecuacion-splib-web} relies on.
 *     When a subclass of {@link SplibWebSecurityConfig} is registered as a bean,
 *     this auto-configuration is skipped.</p>
 *
 * <p><strong>Condition uses {@link SplibWebSecurityConfig} rather than
 *     {@link SecurityFilterChain}.</strong>
 *     Using {@code @ConditionalOnMissingBean(SecurityFilterChain.class)} was unreliable
 *     when the application's {@code @Configuration} class uses
 *     {@code proxyBeanMethods = false} and inherits the {@code @Bean} method from
 *     a non-{@code @Configuration} parent. Checking for {@link SplibWebSecurityConfig}
 *     instead detects the configuration class bean directly and avoids that limitation.</p>
 */
@AutoConfiguration
public class SplibWebSecurityAutoConfiguration {

  /**
   * Provides a permissive {@link SecurityFilterChain} that allows all requests.
   *
   * <p>Applied only when no {@link SplibWebSecurityConfig} subclass bean is present.
   *     Checking for {@link SplibWebSecurityConfig} rather than {@link SecurityFilterChain}
   *     avoids a Spring Boot limitation 
   *     where {@code @ConditionalOnMissingBean(SecurityFilterChain.class)}
   *     fails to detect the filter chain bean when the declaring {@code @Configuration} class
   *     uses {@code proxyBeanMethods = false} and the {@code @Bean} method is inherited
   *     from a non-{@code @Configuration} parent class.</p>
   *
   * @param http the {@link HttpSecurity} to configure
   * @return a {@link SecurityFilterChain} that permits all requests
   * @throws Exception if an error occurs during configuration
   */
  @Bean
  @ConditionalOnMissingBean(SplibWebSecurityConfig.class)
  SecurityFilterChain splibDefaultPermitAllFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
