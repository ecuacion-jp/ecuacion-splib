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
 * Auto-configures a permissive {@link SecurityFilterChain} when no other
 * {@link SecurityFilterChain} bean is present in the application context.
 *
 * <p>This allows applications that use only a subset of {@code ecuacion-splib-web}
 *     (e.g. only Jakarta Validation) to run without defining a {@code SecurityConfig}.
 *     Applications that extend {@link SplibWebSecurityConfig} will have their own
 *     {@link SecurityFilterChain} bean, so this auto-configuration will not be applied.</p>
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
