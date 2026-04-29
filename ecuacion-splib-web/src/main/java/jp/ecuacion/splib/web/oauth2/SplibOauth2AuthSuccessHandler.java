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
package jp.ecuacion.splib.web.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

/**
 * OAuth2 authentication success handler for splib.
 *
 * <p>After Spring Security completes the OAuth2 / OIDC flow, this handler:</p>
 * <ol>
 *   <li>Extracts {@code email} and {@code name} from the {@link OidcUser}
 *       (for Apple first-login, the {@code name} is read from the {@code user}
 *       POST parameter).</li>
 *   <li>Calls {@link SplibOauth2UserHandler#findOrCreateUser} to obtain
 *       the application's {@link UserDetails}.</li>
 *   <li>Replaces the OAuth2 token in the security context with a
 *       {@link UsernamePasswordAuthenticationToken} so that the rest of the
 *       application behaves identically to form-based login.</li>
 *   <li>Calls {@link SplibOauth2UserHandler#afterLoginSuccess} for any
 *       application-specific post-login work (e.g. session attributes).</li>
 *   <li>Redirects to the saved request or default success URL.</li>
 * </ol>
 *
 * <p>This bean is activated only when a {@link SplibOauth2UserHandler} bean
 * is present in the application context.</p>
 */
@Component
@ConditionalOnBean(SplibOauth2UserHandler.class)
public class SplibOauth2AuthSuccessHandler
    extends SavedRequestAwareAuthenticationSuccessHandler {

  private final SplibOauth2UserHandler userHandler;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Constructs a new instance.
   *
   * @param userHandler the application-provided user handler
   */
  public SplibOauth2AuthSuccessHandler(SplibOauth2UserHandler userHandler) {
    this.userHandler = userHandler;
  }

  @SuppressWarnings("null")
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
    OidcUser oidcUser = (OidcUser) oauth2Token.getPrincipal();
    String registrationId = oauth2Token.getAuthorizedClientRegistrationId();

    String email = oidcUser.getEmail();
    String name = resolveName(oidcUser, registrationId, request);
    String provider = registrationId.toUpperCase();

    UserDetails userDetails = userHandler.findOrCreateUser(email, name, provider);

    UsernamePasswordAuthenticationToken newAuth =
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails, null, userDetails.getAuthorities());
    newAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(newAuth);

    userHandler.afterLoginSuccess(request, userDetails);

    super.onAuthenticationSuccess(request, response, authentication);
  }

  /**
   * Resolves the user's display name from the OIDC claims or, for Apple's
   * first-login-only {@code user} POST parameter, from the request body.
   */
  @SuppressWarnings("null")
  private String resolveName(OidcUser oidcUser, String registrationId,
      HttpServletRequest request) {
    // Google provides name in claims
    if (oidcUser.getFullName() != null && !oidcUser.getFullName().isBlank()) {
      return oidcUser.getFullName();
    }

    // Apple provides name only on the first login as a POST parameter
    if ("apple".equalsIgnoreCase(registrationId)) {
      String userParam = request.getParameter("user");
      if (userParam != null) {
        try {
          JsonNode root = objectMapper.readTree(userParam);
          JsonNode nameNode = root.path("name");
          String firstName = nameNode.path("firstName").asText("");
          String lastName = nameNode.path("lastName").asText("");
          String fullName = (firstName + " " + lastName).trim();
          if (!fullName.isBlank()) {
            return fullName;
          }
        } catch (Exception ignored) {
          // malformed JSON: fall through to email-based fallback
        }
      }
    }

    // Fallback: use the local part of the email address
    String email = oidcUser.getEmail();
    return email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
  }
}
