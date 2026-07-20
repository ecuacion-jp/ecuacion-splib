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

import jp.ecuacion.lib.core.logging.DetailLogger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auto-configures a lockdown {@link SecurityFilterChain} fallback
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
 * <p><strong>Fails closed, not open.</strong> Since {@code ecuacion-splib-web} depends on
 *     {@code spring-boot-starter-security} unconditionally, any application that pulls it in
 *     — even one that only wants, say, the Jakarta Validation utilities and never intended to
 *     add real pages — has Spring Security on its classpath and needs some
 *     {@link SecurityFilterChain}. But whether a missing {@link SplibWebSecurityConfig} bean
 *     means "this app deliberately has no pages yet" or "a real page was just added and its
 *     author forgot to wire up security" cannot be told apart from here, and Spring gives no
 *     other signal either way: the application starts up exactly the same in both cases. Denying
 *     every request is the fallback that stays safe under the second reading; the first case
 *     costs nothing extra since there is nothing being served for it to block. Since that first
 *     case is legitimate and this fallback fires on every one of its startups forever, the
 *     accompanying log line ({@link #logFallbackActive}) is written at INFO rather than WARN —
 *     it identifies which situation is happening for whoever goes looking, without implying
 *     something needs fixing on every normal run of a validation-only app.</p>
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

  private final DetailLogger detailLog = new DetailLogger(this);

  /**
   * Provides a lockdown {@link SecurityFilterChain} that denies every request.
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
   * @return a {@link SecurityFilterChain} that denies all requests
   * @throws Exception if an error occurs during configuration
   */
  @Bean
  @ConditionalOnMissingBean(SplibWebSecurityConfig.class)
  SecurityFilterChain splibDefaultDenyAllFilterChain(HttpSecurity http) throws Exception {
    logFallbackActive();
    http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
    return http.build();
  }

  /**
   * Logs that this fallback is active, since it otherwise looks identical to a normal,
   * intentionally configured application — nothing fails, nothing throws, so without this the
   * only symptom of a forgotten {@link SplibWebSecurityConfig} subclass is every page returning
   * access-denied, which is easy to mistake for a to-be-implemented page still being worked on
   * rather than a missing security configuration. Logged at INFO, not WARN: a validation-only
   * application with no {@link SplibWebSecurityConfig} subclass by design hits this on every
   * startup, and that is not a problem to flag.
   */
  private void logFallbackActive() {
    detailLog.info("No SplibWebSecurityConfig subclass bean was found, so "
        + "SplibWebSecurityAutoConfiguration is denying all requests by default. If this "
        + "application serves real pages, define a @Configuration class extending "
        + "SplibWebSecurityConfig (or SplibWebSecurityConfigForNoLogin) to configure real "
        + "authorization instead.");
  }
}
