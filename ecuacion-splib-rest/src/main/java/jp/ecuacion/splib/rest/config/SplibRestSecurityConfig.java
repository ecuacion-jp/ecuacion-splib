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
package jp.ecuacion.splib.rest.config;


import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Provides security config for rest.
 */
public abstract class SplibRestSecurityConfig {

  /**
   * Provides SecurityFilterChain.
   * 
   * @param http http
   * @return SecurityFilterChain
   * @throws Exception Exception
   */
  @Order(8)
  @Bean
  SecurityFilterChain filterChainForApiPublic(HttpSecurity http) throws Exception {
    // MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

    http.securityMatcher("/api/public/**");

    http.httpBasic(basic -> basic.disable());
    http.csrf(csrf -> csrf.disable());

    http.authorizeHttpRequests(requests -> requests
        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/public/**"))
        .permitAll());

    return http.build();
  }

  /**
   * Provides SecurityFilterChain.
   * 
   * @param http http
   * @return SecurityFilterChain
   * @throws Exception Exception
   */
  @Order(9)
  @Bean
  SecurityFilterChain filterChainForApi(HttpSecurity http) throws Exception {
    // MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

    http.securityMatcher("/api/**");

    http.httpBasic(basic -> basic.disable());

    http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());

    return http.build();
  }
}
