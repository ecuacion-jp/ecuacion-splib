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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * web serviceでの使用における、認証が不要なアクセスにて使用する/api/public/** について規定。
 */
public abstract class SplibRestSecurityConfig {

  /** apiとしてはfree accessなので、apiの中では一番順番の低い8番。（9番は後述の通り/api全般に対しての定義なのでそれよりは前の8）。 */
  @Order(8)
  @Bean
  SecurityFilterChain filterChainForApiPublic(HttpSecurity http) throws Exception {
    // MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

    http.securityMatcher("/api/public/**");

    http.httpBasic(basic -> basic.disable());

    http.authorizeHttpRequests(requests -> requests
        .requestMatchers(new AntPathRequestMatcher("/api/public/**")).permitAll());

    return http.build();
  }

  /** /apiで始まるが、/api/public/などでない場合、エラーなのだが、一応apiとしてのエラーを返しておくのが親切なので定義しておく。 */
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
