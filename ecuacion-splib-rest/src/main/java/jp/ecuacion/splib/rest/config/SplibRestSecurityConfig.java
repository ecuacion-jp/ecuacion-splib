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


import jp.ecuacion.splib.rest.apikey.SplibApiKeyAuthenticationFilter;
import jp.ecuacion.splib.rest.apikey.SplibApiKeyComparisonMode;
import jp.ecuacion.splib.rest.apikey.SplibApiKeyExpectedValueProvider;
import jp.ecuacion.splib.rest.apikey.SplibBuiltinApiKeyAuthenticationFilter;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Provides security config for rest.
 *
 * <p>This class's {@link SecurityFilterChain} beans use {@code @Order} values {@code 11}-{@code
 *     19} (currently {@code 11}-{@code 14}, leaving room to insert more later without
 *     renumbering). {@code jp.ecuacion.splib.web.config.SplibWebSecurityConfig} and its siblings
 *     use {@code 21}-{@code 29} — the gap keeps the two modules' chains from ever numerically
 *     colliding when an application uses both, even though today none of their {@code
 *     securityMatcher}s actually overlap either.</p>
 */
public abstract class SplibRestSecurityConfig {

  @Nullable
  private final SplibApiKeyExpectedValueProvider apiKeyExpectedValueProvider;

  /**
   * Constructs a new instance.
   *
   * @param apiKeyExpectedValueProvider the application-supplied provider backing
   *     {@code /api/key/**} authentication, or {@code null} if the application does not use
   *     that prefix — every request to it is then rejected; see
   *     {@link SplibApiKeyExpectedValueProvider}
   */
  protected SplibRestSecurityConfig(
      @Nullable SplibApiKeyExpectedValueProvider apiKeyExpectedValueProvider) {
    this.apiKeyExpectedValueProvider = apiKeyExpectedValueProvider;
  }

  /**
   * Provides an empty {@code UserDetailsService} bean.
   *
   * <p>Rest endpoints defined here never rely on {@code UserDetailsService}-based
   *     authentication ({@code permitAll} or {@code denyAll} only), but without a bean of
   *     this type Spring Boot's {@code UserDetailsServiceAutoConfiguration} kicks in
   *     and logs a "Using generated security password" warning on every startup.
   *     Defining an empty one here suppresses that autoconfiguration.</p>
   *
   * @return UserDetailsService
   */
  @Bean
  UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager();
  }

  /**
   * Provides SecurityFilterChain.
   *
   * <p><strong>Keep {@code /api/public/**} to what's safe to make public.</strong> This path is
   *     {@code permitAll} — reachable without authentication, by anyone, from anywhere — so only
   *     endpoints whose data or actions are safe to expose publicly belong here. An endpoint with
   *     side effects is almost never safe to make public, so in practice this prefix ends up
   *     being read-only (GET/HEAD only). Spring Security enforces none of this: nothing here
   *     stops a {@code @PostMapping} from being added under {@code /api/public/**}; it is a
   *     convention this class assumes but cannot itself enforce.</p>
   *
   * <p>{@code /api/ecuacion-splib/public/**} carries
   *     the same {@code permitAll} policy but is reserved
   *     for {@code ecuacion-splib}'s own built-in endpoints that are safe to expose without
   *     authentication (e.g. {@code AliveCheckController}), so that {@code /api/public/**} stays
   *     exclusively the application's own namespace. Built-in endpoints with side effects (e.g.
   *     {@code SystemErrorController}) instead live under {@code /api/ecuacion-splib/key/**}; see
   *     {@link #filterChainForApiEcuacionSplibKey}.</p>
   *
   * <p>CSRF is disabled here because it only matters when a forged cross-site request can ride
   *     on a credential the browser attaches automatically (e.g. a session cookie) to act on an
   *     authenticated victim's behalf. This path requires no authentication at all, so there is
   *     no such credential for a forged request to exploit.</p>
   *
   * @param http http
   * @return SecurityFilterChain
   * @throws Exception Exception
   */
  @Order(11)
  @Bean
  SecurityFilterChain filterChainForApiPublic(HttpSecurity http) throws Exception {
    // MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

    http.securityMatcher("/api/public/**", "/api/ecuacion-splib/public/**");

    http.httpBasic(basic -> basic.disable());
    http.csrf(csrf -> csrf.disable());

    http.authorizeHttpRequests(requests -> requests
        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/public/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/ecuacion-splib/public/**"))
        .permitAll());

    return http.build();
  }

  /**
   * Provides SecurityFilterChain requiring {@code X-Api-Key} authentication.
   *
   * <p>See {@link SplibApiKeyExpectedValueProvider} for how the expected values are supplied, and
   *     {@link SplibApiKeyComparisonMode} for how each one selects plain-text vs. bcrypt-hashed
   *     comparison.</p>
   *
   * <p><strong>Why CSRF is disabled here, unlike a typical authenticated endpoint.</strong> CSRF
   *     exploits <em>ambient</em> credentials — ones the browser attaches automatically
   *     (cookies) without the page's JavaScript having to know their value. {@code X-Api-Key} is
   *     not ambient: a cross-site page cannot set it without already knowing the key, and by
   *     then it could simply call the API directly without needing the victim's browser at all.
   *     So there is nothing for CSRF protection to add here, regardless of whether the endpoint
   *     underneath is read-only or not (contrast {@link #filterChainForApiPublic}, which is safe
   *     to disable CSRF on because it requires no authentication at all, not because it's
   *     read-only).</p>
   *
   * @param http http
   * @param env source for {@code jp.ecuacion.splib.rest.api-key.rate-limit.*} (see
   *     {@link SplibApiKeyAuthenticationFilter})
   * @return SecurityFilterChain
   * @throws Exception Exception
   */
  @Order(12)
  @Bean
  SecurityFilterChain filterChainForApiKey(HttpSecurity http, Environment env) throws Exception {
    http.securityMatcher("/api/key/**");

    http.httpBasic(basic -> basic.disable());
    http.csrf(csrf -> csrf.disable());

    http.addFilterBefore(new SplibApiKeyAuthenticationFilter(apiKeyExpectedValueProvider, env),
        UsernamePasswordAuthenticationFilter.class);

    // The filter above already rejects (401) any request that fails API-key authentication, so
    // authorization here only needs to admit requests that got past it.
    http.authorizeHttpRequests(requests -> requests
        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/api/key/**"))
        .permitAll());

    return http.build();
  }

  /**
   * Provides SecurityFilterChain requiring {@code X-Api-Key} authentication for
   * {@code ecuacion-splib}'s own built-in endpoints (e.g. {@code SystemErrorController},
   * {@code ClearPropertiesCacheController}).
   *
   * <p>Unlike {@link #filterChainForApiKey}, the expected value here is not application-supplied
   *     — it's read directly from {@code jp.ecuacion.splib.rest.builtin-api-key.password-plain}
   *     or {@code jp.ecuacion.splib.rest.builtin-api-key.password-bcrypt} (exactly one expected
   *     to be set); see {@link SplibBuiltinApiKeyAuthenticationFilter}. This key is independent
   *     of {@link #filterChainForApiKey}'s {@code /api/key/**} keys — the two are registered and
   *     rotated separately.</p>
   *
   * <p>CSRF is disabled here for the same reason as {@link #filterChainForApiKey}: {@code
   *     X-Api-Key} is not a credential the browser attaches automatically, so there is nothing
   *     for CSRF protection to add.</p>
   *
   * @param http http
   * @param env source for {@code jp.ecuacion.splib.rest.builtin-api-key.rate-limit.*} (see
   *     {@link SplibBuiltinApiKeyAuthenticationFilter})
   * @return SecurityFilterChain
   * @throws Exception Exception
   */
  @Order(13)
  @Bean
  SecurityFilterChain filterChainForApiEcuacionSplibKey(HttpSecurity http, Environment env)
      throws Exception {
    http.securityMatcher("/api/ecuacion-splib/key/**");

    http.httpBasic(basic -> basic.disable());
    http.csrf(csrf -> csrf.disable());

    http.addFilterBefore(new SplibBuiltinApiKeyAuthenticationFilter(env),
        UsernamePasswordAuthenticationFilter.class);

    // The filter above already rejects (401) any request that fails API-key authentication, so
    // authorization here only needs to admit requests that got past it.
    http.authorizeHttpRequests(requests -> requests
        .requestMatchers(
            PathPatternRequestMatcher.withDefaults().matcher("/api/ecuacion-splib/key/**"))
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
  @Order(14)
  @Bean
  SecurityFilterChain filterChainForApi(HttpSecurity http) throws Exception {
    // MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

    http.securityMatcher("/api/**");

    http.httpBasic(basic -> basic.disable());

    http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());

    return http.build();
  }

  /**
   * Provides a last-resort {@code denyAll} {@link SecurityFilterChain} for every path not matched
   * by any of this class's other chains (i.e. anything outside {@code /api/**}).
   *
   * <p>Defining any custom {@code SecurityFilterChain} bean disables Spring Boot's own default
   *     security chain entirely. If an application uses {@code ecuacion-splib-rest} without also
   *     using {@code ecuacion-splib-web} (whose {@code SplibWebSecurityConfig} normally provides
   *     this same role for non-{@code /api} pages), a path that happens to match none of the
   *     chains above — e.g. an endpoint added later outside {@code /api/**}, or {@code /error} —
   *     would otherwise match <em>no</em> {@code SecurityFilterChain} at all and bypass Spring
   *     Security entirely (no authentication, no authorization, not even CSRF), rather than
   *     being denied. This chain closes that gap.</p>
   *
   * <p><strong>Left without an explicit {@code @Order}</strong> (defaulting to
   *     {@code Ordered.LOWEST_PRECEDENCE}), deliberately, so it stays strictly lower priority than
   *     {@code jp.ecuacion.splib.web.config.SplibWebSecurityConfig#filterChain}'s explicit
   *     {@code @Order(29)}. That way, an application using both modules together still gets that
   *     class's real {@code permitAll}/{@code denyAll} policy for its non-{@code /api} pages —
   *     this chain never actually matches anything in that case, since
   *     {@code SplibWebSecurityConfig}'s own {@code anyRequest()} chain is evaluated first and
   *     already covers everything reaching this point. Giving this chain any explicit, finite
   *     {@code @Order} instead would make it outrank that class's chain and blanket-deny the
   *     entire non-{@code /api} application by mistake.</p>
   *
   * @param http http
   * @return SecurityFilterChain
   * @throws Exception Exception
   */
  @Bean
  SecurityFilterChain filterChainCatchAll(HttpSecurity http) throws Exception {
    http.securityMatcher("/**");

    http.httpBasic(basic -> basic.disable());

    http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());

    return http.build();
  }
}
