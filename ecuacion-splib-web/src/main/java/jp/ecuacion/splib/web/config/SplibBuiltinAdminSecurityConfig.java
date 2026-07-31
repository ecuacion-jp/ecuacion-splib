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

import java.util.List;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver.Outcome;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver.Result;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

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
 * <p><strong>Credential property.</strong> Exactly one of the following must be set:</p>
 * <ul>
 * <li>{@code jp.ecuacion.splib.web.builtin-admin-login.password-plain} — the password itself.</li>
 * <li>{@code jp.ecuacion.splib.web.builtin-admin-login.password-bcrypt} — a bcrypt hash of it.</li>
 * </ul>
 * 
 * <p>See {@link SplibHashedPropertyResolver} for the shared convention this follows (also used by
 *     {@code ecuacion-splib-rest}'s built-in API key).</p>
 *
 * <p><strong>Fails closed, not open.</strong> If neither property is set, no login is possible —
 *     the safe default for an app that doesn't use this feature — and this is indistinguishable
 *     from a wrong password, mirroring how REST's equivalent {@code /api/ecuacion-splib/key/**}
 *     tier rejects every request when neither of its own credential properties is set (see
 *     {@code SplibBuiltinApiKeyAuthenticationFilter}). If <em>both</em> are set, that is instead
 *     a misconfiguration only whoever controls {@code application.properties} could cause, so it
 *     is surfaced distinctly, via a dedicated login-page error message rather than the generic
 *     "wrong credentials" one.</p>
 */
@Configuration
public class SplibBuiltinAdminSecurityConfig {

  /**
   * Fixed username for {@code ecuacion-splib}'s built-in admin login.
   */
  public static final String BUILTIN_ADMIN_USERNAME = "ecuacion-splib";

  private static final String PROPERTY_PREFIX = "jp.ecuacion.splib.web.builtin-admin-login";

  private static final String LOGIN_PAGE = "/ecuacion-splib/public/adminLogin/page";

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

    http.authenticationProvider(builtinAdminAuthenticationProvider());

    http.formLogin(login -> login.loginPage(LOGIN_PAGE)
        .loginProcessingUrl("/ecuacion-splib/public/adminLogin/action")
        .usernameParameter("builtinAdminLogin.username")
        .passwordParameter("builtinAdminLogin.password")
        .defaultSuccessUrl("/ecuacion-splib/admin/config/page", true)
        .failureHandler(builtinAdminFailureHandler()));

    http.authorizeHttpRequests(
        requests -> requests.requestMatchers("/ecuacion-splib/public/adminLogin/**").permitAll()
            .anyRequest().authenticated());

    http.logout(logout -> logout.logoutUrl("/ecuacion-splib/adminLogout")
        .logoutSuccessUrl(LOGIN_PAGE + "?logoutDone"));

    return http.build();
  }

  /**
   * Authenticates against the fixed {@link #BUILTIN_ADMIN_USERNAME} and whichever single
   * {@code password-plain} / {@code password-bcrypt} property is configured, resolved fresh on
   * every attempt (so a property change via {@code PropertiesFileUtil}'s cache-clearing takes
   * effect without a restart).
   */
  private AuthenticationProvider builtinAdminAuthenticationProvider() {
    return new AuthenticationProvider() {

      @Override
      public Authentication authenticate(Authentication authentication)
          throws AuthenticationException {
        String presentedUsername = String.valueOf(authentication.getPrincipal());
        String presentedPassword = String.valueOf(authentication.getCredentials());

        Result result =
            SplibHashedPropertyResolver.authenticate(PROPERTY_PREFIX, presentedPassword);

        if (result.getOutcome() == Outcome.MISCONFIGURED) {
          throw new BuiltinCredentialMisconfiguredException("More than one of "
              + result.getConfiguredKeys() + " is set; exactly one is expected.");
        }

        if (!BUILTIN_ADMIN_USERNAME.equals(presentedUsername)
            || result.getOutcome() != Outcome.MATCHED) {
          throw new BadCredentialsException("Bad credentials");
        }

        return UsernamePasswordAuthenticationToken.authenticated(presentedUsername, null,
            List.of(new SimpleGrantedAuthority("ROLE_BUILTIN_ADMIN")));
      }

      @Override
      public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
      }
    };
  }

  /**
   * Redirects to the login page with {@code ?credentialMisconfigured} for a
   * {@link BuiltinCredentialMisconfiguredException}, or {@code ?error} for anything else (wrong
   * username/password), so the two cases show distinct messages.
   */
  private AuthenticationFailureHandler builtinAdminFailureHandler() {
    RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    return (request, response, exception) -> {
      String query =
          exception instanceof BuiltinCredentialMisconfiguredException ? "?credentialMisconfigured"
              : "?error";
      redirectStrategy.sendRedirect(request, response, LOGIN_PAGE + query);
    };
  }

  /**
   * Signals that more than one credential property is configured for the same login — a
   * misconfiguration only whoever controls {@code application.properties} could cause, not
   * something an external caller can trigger by guessing.
   */
  private static final class BuiltinCredentialMisconfiguredException
      extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    BuiltinCredentialMisconfiguredException(String message) {
      super(message);
    }
  }
}
