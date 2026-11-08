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

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver.Outcome;
import jp.ecuacion.splib.core.util.SplibHashedPropertyResolver.Result;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * Provides security config for {@code ecuacion-splib}'s own built-in admin pages
 * (e.g. {@link jp.ecuacion.splib.web.controller.ConfigController}).
 *
 * <p>Unlike {@link SplibWebSecurityConfigForAdmin}, this class is concrete and registered
 *     automatically — apps do not need to subclass it. It protects
 *     {@code /ecuacion-splib/admin/**} with HTTP Basic authentication against a single fixed
 *     built-in user, independent of whatever {@code UserDetailsService} the app itself registers
 *     for its own login.</p>
 *
 * <p><strong>Independent of the app's own login state.</strong> The authentication is neither
 *     read from nor stored in the HTTP session
 *     (see {@link RequestAttributeSecurityContextRepository}), so:</p>
 * <ul>
 * <li>a user logged in to the app itself is not let in, and is asked for the built-in admin
 *     credentials just like an anonymous user,</li>
 * <li>authenticating here neither replaces nor logs out the app's own login, and the app's own
 *     pages never see the built-in admin as a logged-in user,</li>
 * <li>it works the same in an app that has no login at all (every page public).</li>
 * </ul>
 * 
 * <p>Since the browser keeps re-sending the credentials until it is closed, there is no logout.
 *     CSRF protection stays enabled for the same reason.</p>
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
 * <p><strong>Fails closed, not open.</strong> If neither property is set, no authentication is
 *     possible — the safe default for an app that doesn't use this feature — and this is
 *     indistinguishable from a wrong password, mirroring how REST's equivalent
 *     {@code /api/ecuacion-splib/key/**} tier rejects every request when neither of its own
 *     credential properties is set (see {@code SplibBuiltinApiKeyAuthenticationFilter}). If
 *     <em>both</em> are set, that is instead a misconfiguration only whoever controls
 *     {@code application.properties} could cause, so, also like REST, it is logged as an error
 *     and answered with 500 rather than the generic 401.</p>
 */
@Configuration
public class SplibBuiltinAdminSecurityConfig {

  /**
   * Fixed username for {@code ecuacion-splib}'s built-in admin authentication.
   */
  public static final String BUILTIN_ADMIN_USERNAME = "ecuacion-splib";

  private static final String PROPERTY_PREFIX = "jp.ecuacion.splib.web.builtin-admin-login";

  private static final String REALM_NAME = "ecuacion-splib";

  private final DetailLogger detailLog = new DetailLogger(this);

  /**
   * Provides SecurityFilterChain for {@code ecuacion-splib}'s own built-in admin pages.
   *
   * @param http http
   * @return SecurityFilterChain
   * @throws Exception Exception
   */
  @Order(22)
  @Bean
  SecurityFilterChain filterChainForBuiltinAdmin(HttpSecurity http) throws Exception {

    http.securityMatcher("/ecuacion-splib/admin/**");

    // Keeps the authentication, and the request saved on a 401, out of the session shared with
    // the app's own login.
    http.securityContext(context -> context
        .securityContextRepository(new RequestAttributeSecurityContextRepository()));
    http.requestCache(cache -> cache.disable());

    // A ProviderManager without a parent, so a failure here never falls back to the app's own
    // UserDetailsService (an app user's credentials must not authenticate here).
    http.authenticationManager(new ProviderManager(builtinAdminAuthenticationProvider()));

    http.httpBasic(basic -> basic.realmName(REALM_NAME)
        .authenticationEntryPoint(builtinAdminAuthenticationEntryPoint()));

    http.authorizeHttpRequests(requests -> requests.anyRequest().hasRole("BUILTIN_ADMIN"));

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
   * Answers 401 with the Basic challenge, or, for a
   * {@link BuiltinCredentialMisconfiguredException}, logs it and answers 500, so the
   * misconfiguration is not mistaken for a wrong password.
   */
  private AuthenticationEntryPoint builtinAdminAuthenticationEntryPoint() {
    BasicAuthenticationEntryPoint basicEntryPoint = new BasicAuthenticationEntryPoint();
    basicEntryPoint.setRealmName(REALM_NAME);
    return (request, response, exception) -> {
      if (exception instanceof BuiltinCredentialMisconfiguredException) {
        detailLog.error(exception.getMessage() + " Rejecting request to "
            + request.getRequestURI() + ".");
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "ecuacion-splib built-in admin authentication is misconfigured.");
        return;
      }

      basicEntryPoint.commence(request, response, exception);
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
