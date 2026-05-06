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
 * when no other {@link SecurityFilterChain} bean is present in the application context.
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
 *     Applications that extend it will register their own {@link SecurityFilterChain} bean,
 *     so this auto-configuration will be skipped.</p>
 *
 * <p><strong>Coexistence with a hand-rolled
 *     {@code @Configuration + @EnableWebSecurity + @Bean SecurityFilterChain} is
 *     not supported.</strong> The {@code @ConditionalOnMissingBean} guard cannot be
 *     relied upon in that arrangement: depending on bean-definition processing order,
 *     both filter chains may end up registered, which causes
 *     {@code UnreachableFilterChainException}
 *     (a filter chain matching {@code anyRequest()} has already been configured).
 *     If you need a custom security configuration in a web application,
 *     extend {@link SplibWebSecurityConfig} instead of declaring
 *     a separate {@link SecurityFilterChain} bean.</p>
 */
@AutoConfiguration
public class SplibWebSecurityAutoConfiguration {

  /**
   * Provides a permissive {@link SecurityFilterChain} that allows all requests.
   *
   * <p>Applied only when no other {@link SecurityFilterChain} bean is present.</p>
   *
   * @param http the {@link HttpSecurity} to configure
   * @return a {@link SecurityFilterChain} that permits all requests
   * @throws Exception if an error occurs during configuration
   */
  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  SecurityFilterChain splibDefaultPermitAllFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
