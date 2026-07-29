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

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Provides security config for {@code ecuacion-splib}'s own built-in admin pages
 * (e.g. {@link jp.ecuacion.splib.web.controller.ConfigController}).
 *
 * <p>Unlike {@link SplibWebSecurityConfigForAdmin}, this class is concrete and registered
 *     automatically — apps do not need to subclass it. It protects
 *     {@code /ecuacion-splib/admin/**} (login required) and serves its own login page at
 *     {@code /ecuacion-splib/public/adminLogin/**} (no login required, consistent with the
 *     {@code public}/{@code admin} split used elsewhere under {@code /ecuacion-splib/**}), with
 *     a single fixed built-in user independent of whatever {@code UserDetailsService} the app
 *     itself registers for its own login.</p>
 *
 * <p><strong>Fails closed, not open.</strong> If
 *     {@code jp.ecuacion.splib.web.builtin-admin-login.password-hash} is not set, no user is
 *     registered and login is therefore impossible — the safe default for an app that doesn't
 *     use this feature, mirroring how a {@code null}
 *     {@code SplibBuiltinApiKeyExpectedValueProvider} makes REST's equivalent
 *     {@code /api/ecuacion-splib/key/**} tier reject every request.</p>
 */
@Configuration
public class SplibBuiltinAdminSecurityConfig {

  /**
   * Fixed username for {@code ecuacion-splib}'s built-in admin login.
   */
  public static final String BUILTIN_ADMIN_USERNAME = "ecuacion-splib";

  @Nullable
  @Value("${jp.ecuacion.splib.web.builtin-admin-login.password-hash:#{null}}")
  private String passwordHash;

  /**
   * Provides SecurityFilterChain for {@code ecuacion-splib}'s own built-in admin pages.
   *
   * @param http http
   * @return SecurityFilterChain
   * @throws Exception Exception
   */
  @Order(12)
  @Bean
  SecurityFilterChain filterChainForBuiltinAdmin(HttpSecurity http) throws Exception {

    http.securityMatcher("/ecuacion-splib/public/adminLogin/**", "/ecuacion-splib/admin/**",
        "/ecuacion-splib/adminLogout");

    http.httpBasic(basic -> basic.disable());

    DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider(builtinAdminUserDetailsService());
    provider.setPasswordEncoder(new BCryptPasswordEncoder());
    http.authenticationProvider(provider);

    http.formLogin(login -> login.loginPage("/ecuacion-splib/public/adminLogin/page")
        .loginProcessingUrl("/ecuacion-splib/public/adminLogin/action")
        .defaultSuccessUrl("/ecuacion-splib/admin/config/page", true)
        .failureUrl("/ecuacion-splib/public/adminLogin/page?error"));

    http.authorizeHttpRequests(requests -> requests
        .requestMatchers("/ecuacion-splib/public/adminLogin/**").permitAll()
        .anyRequest().authenticated());

    http.logout(logout -> logout.logoutUrl("/ecuacion-splib/adminLogout")
        .logoutSuccessUrl("/ecuacion-splib/public/adminLogin/page?logoutDone"));

    return http.build();
  }

  /**
   * Builds the single fixed builtin admin user from
   * {@code jp.ecuacion.splib.web.builtin-admin-login.password-hash}.
   *
   * <p>Returns a {@code UserDetailsService} backed by no user at all when the property is
   *     unset, so login always fails rather than falling back to some default credential.</p>
   */
  private InMemoryUserDetailsManager builtinAdminUserDetailsService() {
    if (passwordHash == null) {
      return new InMemoryUserDetailsManager();
    }

    UserDetails user = User.withUsername(BUILTIN_ADMIN_USERNAME).password(passwordHash)
        .roles("BUILTIN_ADMIN").build();
    return new InMemoryUserDetailsManager(user);
  }
}
