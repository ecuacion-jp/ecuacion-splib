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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * OAuth2 authentication success handler for splib.
 *
 * <p>After Spring Security completes the OAuth2 / OIDC flow, this handler:</p>
 * <ol>
 *   <li>Extracts {@code email}, {@code name} and {@code sub} from the {@link OidcUser}
 *       as {@link SplibOauth2UserInfo}
 *       (for Apple first-login, the {@code name} is read from the {@code user}
 *       POST parameter). {@code email_verified} has already been checked by
 *       {@link SplibOauth2OidcUserService}.</li>
 *   <li>Calls {@link SplibOauth2UserHandler#findOrCreateUser} to obtain
 *       the application's {@link UserDetails}.
 *       When the handler is a {@link SplibOauth2UserHandlerWithRegistrationConfirm},
 *       {@link SplibOauth2UserHandlerWithRegistrationConfirm#findUser} is called instead,
 *       and if it returns {@code null}, the user is redirected to the registration confirm page
 *       without being logged in (the following steps are skipped).</li>
 *   <li>Rejects the login when the {@link UserDetails} is locked, disabled or expired,
 *       or when the app throws {@link SplibOauth2LoginRejectedException} in the step above
 *       (the following steps are skipped).</li>
 *   <li>Replaces the OAuth2 token in the security context with a
 *       {@link UsernamePasswordAuthenticationToken} so that the rest of the
 *       application behaves identically to form-based login.</li>
 *   <li>Calls {@link SplibOauth2UserHandler#afterLoginSuccess} for any
 *       application-specific post-login work (e.g. session attributes).</li>
 *   <li>Redirects to the saved request or default success URL.</li>
 * </ol>
 *
 * <p>When the login is rejected, the user is not logged in,
 *     {@link SplibOauth2UserHandler#afterLoginSuccess} is not called,
 *     and the user is redirected to the login page with {@code ?oauth2Rejected}.
 *     The reason is written to the log with {@code INFO}, and not shown to the user.</p>
 *
 * <p>This bean is registered by {@link SplibOauth2Config}.</p>
 */
public class SplibOauth2AuthSuccessHandler
    extends SavedRequestAwareAuthenticationSuccessHandler {

  /** The url parameter added to the login page when the login is rejected. */
  static final String REJECTED_PARAM = "oauth2Rejected";

  private final DetailLogger detailLog = new DetailLogger(this);

  private final SplibOauth2UserHandler userHandler;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * The same as the one set in {@link jp.ecuacion.splib.web.config.SplibOauth2WebSecurityConfig}.
   */
  private final OAuth2AuthorizedClientRepository authorizedClientRepository =
      new HttpSessionOAuth2AuthorizedClientRepository();

  private String loginPage = "/public/login/page";

  /** The same as Spring Security's default for servlet apps. */
  private final SecurityContextRepository securityContextRepository =
      new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
          new HttpSessionSecurityContextRepository());

  private final RequestCache requestCache = new HttpSessionRequestCache();

  /**
   * Applied when logging in after the registration confirmation, which is outside of
   * Spring Security's login filters. The same as the ones Spring Security applies on login.
   */
  private final SessionAuthenticationStrategy sessionAuthenticationStrategy =
      new CompositeSessionAuthenticationStrategy(
          List.of(new ChangeSessionIdAuthenticationStrategy(),
              new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository())));

  /**
   * Constructs a new instance.
   *
   * @param userHandler the application-provided user handler
   */
  public SplibOauth2AuthSuccessHandler(SplibOauth2UserHandler userHandler) {
    this.userHandler = userHandler;
    setRequestCache(requestCache);
  }

  /**
   * Sets the login page, to which the user is redirected when the login is rejected.
   *
   * @param loginPage the path of the login page, without the context path
   */
  public void setLoginPage(String loginPage) {
    this.loginPage = loginPage;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
    OidcUser oidcUser = (OidcUser) oauth2Token.getPrincipal();
    String registrationId = oauth2Token.getAuthorizedClientRegistrationId();

    String email = Objects.requireNonNull(oidcUser.getEmail());
    String name = resolveName(oidcUser, registrationId, email, request);
    String provider = registrationId.toUpperCase(Locale.ROOT);
    String subject = Objects.requireNonNull(oidcUser.getSubject());
    SplibOauth2UserInfo userInfo = new SplibOauth2UserInfo(email, name, provider, subject);

    UserDetails userDetails;
    try {
      if (userHandler instanceof SplibOauth2UserHandlerWithRegistrationConfirm confirmHandler) {
        UserDetails foundUser = confirmHandler.findUser(userInfo);
        if (foundUser == null) {
          redirectToRegistrationConfirmPage(request, response, oauth2Token,
              new SplibOauth2PendingRegistration(userInfo),
              confirmHandler.getRegistrationConfirmPage());
          return;
        }
        userDetails = foundUser;

      } else {
        userDetails = userHandler.findOrCreateUser(userInfo);
      }

    } catch (SplibOauth2LoginRejectedException ex) {
      getRedirectStrategy().sendRedirect(request, response,
          reject(request, response, oauth2Token, userInfo, String.valueOf(ex.getMessage())));
      return;
    }

    String rejectionReason = accountStatusRejectionReason(userDetails);
    if (rejectionReason != null) {
      getRedirectStrategy().sendRedirect(request, response,
          reject(request, response, oauth2Token, userInfo, rejectionReason));
      return;
    }

    // Discard the one left by an abandoned registration confirmation.
    removePendingRegistration(request);

    login(request, response, createAuthentication(request, userDetails), userDetails);

    super.onAuthenticationSuccess(request, response, authentication);
  }

  /**
   * Logs in the user registered on the registration confirm page.
   *
   * @return the url to redirect to
   */
  String loginAfterRegistration(HttpServletRequest request, HttpServletResponse response,
      UserDetails userDetails) {
    String rejectionReason = accountStatusRejectionReason(userDetails);
    if (rejectionReason != null) {
      return rejectLogin(request, response, rejectionReason);
    }

    removePendingRegistration(request);

    UsernamePasswordAuthenticationToken auth = createAuthentication(request, userDetails);
    sessionAuthenticationStrategy.onAuthentication(auth, request, response);
    login(request, response, auth, userDetails);
    clearAuthenticationAttributes(request);

    SavedRequest savedRequest = requestCache.getRequest(request, response);
    return savedRequest == null ? determineTargetUrl(request, response)
        : savedRequest.getRedirectUrl();
  }

  /**
   * Rejects the login on the registration confirm page.
   *
   * @return the url to redirect to
   */
  String rejectLogin(HttpServletRequest request, HttpServletResponse response, String reason) {
    SplibOauth2PendingRegistration pending = getPendingRegistration(request);
    return reject(request, response, null, pending == null ? null : pending.userInfo(), reason);
  }

  /**
   * Gets the pending registration from the session.
   */
  static @Nullable SplibOauth2PendingRegistration getPendingRegistration(
      HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session == null ? null
        : (SplibOauth2PendingRegistration) session
            .getAttribute(SplibOauth2PendingRegistration.SESSION_KEY);
  }

  /**
   * Removes the pending registration from the session.
   */
  static void removePendingRegistration(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(SplibOauth2PendingRegistration.SESSION_KEY);
    }
  }

  /**
   * Returns the reason when the account status doesn't allow the login, otherwise {@code null}.
   *
   * <p>The items and the order are the same as the ones Spring Security checks
   *     before the id / password login.
   *     {@link UserDetails#isCredentialsNonExpired()} is not checked
   *     since the credentials are not used in the OAuth2 login.</p>
   */
  private static @Nullable String accountStatusRejectionReason(UserDetails userDetails) {
    if (!userDetails.isAccountNonLocked()) {
      return "the account is locked.";
    }
    if (!userDetails.isEnabled()) {
      return "the account is disabled.";
    }
    if (!userDetails.isAccountNonExpired()) {
      return "the account has expired.";
    }
    return null;
  }

  /**
   * Clears what the login has left, logs the reason and returns the url to redirect to.
   *
   * @param oauth2Token the OAuth2 authentication to clear the authorized client of,
   *     {@code null} if it's already cleared
   * @param userInfo the user rejected, {@code null} if unknown
   */
  private String reject(HttpServletRequest request, HttpServletResponse response,
      @Nullable OAuth2AuthenticationToken oauth2Token, @Nullable SplibOauth2UserInfo userInfo,
      String reason) {
    clearLogin(request, response, oauth2Token);

    detailLog.info("OAuth2 login rejected. provider: "
        + (userInfo == null ? null : userInfo.provider()) + ", subject: "
        + (userInfo == null ? null : userInfo.subject()) + ", reason: " + reason);

    return loginPage + "?" + REJECTED_PARAM;
  }

  /**
   * Clears the security context, the OAuth2 authorized client and the pending registration.
   *
   * <p>Spring Security has already saved the OAuth2 authentication and the authorized client
   *     to the session before calling this handler.</p>
   *
   * @param oauth2Token the OAuth2 authentication to clear the authorized client of,
   *     {@code null} if it's already cleared
   */
  private void clearLogin(HttpServletRequest request, HttpServletResponse response,
      @Nullable OAuth2AuthenticationToken oauth2Token) {
    SecurityContext emptyContext =
        SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
    SecurityContextHolder.getContextHolderStrategy().clearContext();
    securityContextRepository.saveContext(emptyContext, request, response);

    if (oauth2Token != null) {
      authorizedClientRepository.removeAuthorizedClient(
          oauth2Token.getAuthorizedClientRegistrationId(), oauth2Token, request, response);
    }

    removePendingRegistration(request);
    clearAuthenticationAttributes(request);
  }

  private UsernamePasswordAuthenticationToken createAuthentication(HttpServletRequest request,
      UserDetails userDetails) {
    UsernamePasswordAuthenticationToken auth = UsernamePasswordAuthenticationToken
        .authenticated(userDetails, null, userDetails.getAuthorities());
    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    return auth;
  }

  /**
   * Replaces the security context with the one for {@code auth} and saves it to the session
   * so that the rest of the application behaves identically to form-based login.
   */
  private void login(HttpServletRequest request, HttpServletResponse response,
      UsernamePasswordAuthenticationToken auth, UserDetails userDetails) {
    SecurityContext context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.getContextHolderStrategy().setContext(context);
    securityContextRepository.saveContext(context, request, response);

    userHandler.afterLoginSuccess(request, userDetails);
  }

  /**
   * Redirects to the registration confirm page without logging in.
   *
   * <p>Spring Security has already saved the OAuth2 authentication to the session before calling
   *     this handler, so it's explicitly cleared here.</p>
   */
  private void redirectToRegistrationConfirmPage(HttpServletRequest request,
      HttpServletResponse response, OAuth2AuthenticationToken oauth2Token,
      SplibOauth2PendingRegistration pendingRegistration, String confirmPage) throws IOException {
    clearLogin(request, response, oauth2Token);

    request.getSession().setAttribute(SplibOauth2PendingRegistration.SESSION_KEY,
        pendingRegistration);

    getRedirectStrategy().sendRedirect(request, response, confirmPage);
  }

  /**
   * Resolves the user's display name from the OIDC claims or, for Apple's
   * first-login-only {@code user} POST parameter, from the request body.
   */
  private String resolveName(OidcUser oidcUser, String registrationId, String email,
      HttpServletRequest request) {
    // Google provides name in claims
    String fullNameClaim = oidcUser.getFullName();
    if (fullNameClaim != null && !fullNameClaim.isBlank()) {
      return fullNameClaim;
    }

    // Apple provides name only on the first login as a POST parameter
    if ("apple".equalsIgnoreCase(registrationId)) {
      String userParam = request.getParameter("user");
      if (userParam != null) {
        try {
          JsonNode root = objectMapper.readTree(userParam);
          JsonNode nameNode = root.path("name");
          JsonNode fnNode = nameNode.path("firstName");
          JsonNode lnNode = nameNode.path("lastName");
          String firstName = fnNode.stringValue("");
          String lastName = lnNode.stringValue("");
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
    return email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
  }
}
