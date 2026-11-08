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

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

/** Tests for {@link SplibOauth2AuthSuccessHandler} and
 * {@link SplibOauth2RegistrationConfirmService}. */
@DisplayName("SplibOauth2AuthSuccessHandler")
class SplibOauth2AuthSuccessHandlerTest {

  private static final String CONTEXT_KEY =
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
  private static final String DEFAULT_URL = "/account/home/page";
  private static final String CONFIRM_PAGE = "/public/registrationConfirm/page";
  private static final String LOGIN_PAGE = "/public/login/page";
  private static final String REJECTED_URL = LOGIN_PAGE + "?oauth2Rejected";

  private final HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository =
      new HttpSessionOAuth2AuthorizedClientRepository();

  private final Logger logger =
      (Logger) LoggerFactory.getLogger(SplibOauth2AuthSuccessHandler.class.getName());
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  private MockHttpSession session = new MockHttpSession();

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.INFO);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    logger.detachAppender(appender);
    logger.setLevel(null);
  }

  /** Simulates the state right after Spring Security's OAuth2 login filter has run. */
  private MockHttpServletRequest oauth2CallbackRequest(OAuth2AuthenticationToken token) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login/oauth2/code/x");
    request.setSession(session);
    SecurityContext context = new SecurityContextImpl(token);
    SecurityContextHolder.setContext(context);
    session.setAttribute(CONTEXT_KEY, context);
    authorizedClientRepository.saveAuthorizedClient(authorizedClient(token), token, request,
        new MockHttpServletResponse());
    return request;
  }

  private static OAuth2AuthorizedClient authorizedClient(OAuth2AuthenticationToken token) {
    String registrationId = token.getAuthorizedClientRegistrationId();
    ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
        .clientId("client").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://example.com/login/oauth2/code/" + registrationId)
        .authorizationUri("https://example.com/auth").tokenUri("https://example.com/token")
        .build();
    OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
        "access", Instant.now(), Instant.now().plusSeconds(60));
    return new OAuth2AuthorizedClient(registration, token.getName(), accessToken);
  }

  private @Nullable OAuth2AuthorizedClient savedAuthorizedClient(MockHttpServletRequest request,
      OAuth2AuthenticationToken token) {
    return authorizedClientRepository.loadAuthorizedClient(
        token.getAuthorizedClientRegistrationId(), token, request);
  }

  /** The {@link UserDetails} whose status doesn't allow the login. */
  @SuppressWarnings("null")
  static Stream<Arguments> rejectedUserDetails() {
    return Stream.of(
        Arguments.of(User.withUserDetails(userDetails()).accountLocked(true).build(),
            "the account is locked."),
        Arguments.of(User.withUserDetails(userDetails()).disabled(true).build(),
            "the account is disabled."),
        Arguments.of(User.withUserDetails(userDetails()).accountExpired(true).build(),
            "the account has expired."));
  }

  private void assertRejected(MockHttpServletRequest request, @Nullable String redirectedUrl,
      String reason) {
    assertThat(redirectedUrl).isEqualTo(REJECTED_URL);
    assertThat(loggedInPrincipal()).isNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(SplibOauth2AuthSuccessHandler.getPendingRegistration(request)).isNull();

    assertThat(appender.list).hasSize(1);
    ILoggingEvent log = appender.list.get(0);
    assertThat(log.getLevel()).isEqualTo(Level.INFO);
    assertThat(log.getFormattedMessage()).contains("OAuth2 login rejected.")
        .contains("provider: GOOGLE").contains("subject: sub1").contains("reason: " + reason);
  }

  private static OAuth2AuthenticationToken token(String registrationId, @Nullable String name) {
    OidcIdToken.Builder builder =
        OidcIdToken.withTokenValue("token").claim("sub", "sub1").claim("email", "a@example.com");
    if (name != null) {
      builder.claim("name", name);
    }
    DefaultOidcUser user = new DefaultOidcUser(AuthorityUtils.NO_AUTHORITIES, builder.build());
    return new OAuth2AuthenticationToken(user, AuthorityUtils.NO_AUTHORITIES, registrationId);
  }

  private static UserDetails userDetails() {
    return User.withUsername("a@example.com").password("").roles("USER").build();
  }

  private static SplibOauth2AuthSuccessHandler handler(SplibOauth2UserHandler userHandler) {
    SplibOauth2AuthSuccessHandler handler = new SplibOauth2AuthSuccessHandler(userHandler);
    handler.setDefaultTargetUrl(DEFAULT_URL);
    handler.setLoginPage(LOGIN_PAGE);
    return handler;
  }

  private @Nullable Object loggedInPrincipal() {
    SecurityContext context = (SecurityContext) session.getAttribute(CONTEXT_KEY);
    if (context == null || context.getAuthentication() == null) {
      return null;
    }
    assertThat(context.getAuthentication()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
    return Objects.requireNonNull(context.getAuthentication()).getPrincipal();
  }

  /** Records calls for assertions. */
  private static class FindOrCreateHandler implements SplibOauth2UserHandler {
    private final @Nullable UserDetails user;
    private final List<String> calls = new ArrayList<>();

    /** Throws {@link SplibOauth2LoginRejectedException} when {@code user} is {@code null}. */
    FindOrCreateHandler(@Nullable UserDetails user) {
      this.user = user;
    }

    @SuppressWarnings("null")
    @Override
    public UserDetails findOrCreateUser(SplibOauth2UserInfo userInfo) {
      calls.add("findOrCreateUser:" + userInfo);
      if (user == null) {
        throw new SplibOauth2LoginRejectedException("rejected by the app.");
      }
      return user;
    }

    @Override
    public void afterLoginSuccess(HttpServletRequest request, UserDetails userDetails) {
      calls.add("afterLoginSuccess:" + userDetails.getUsername());
    }
  }

  /** Records calls for assertions. */
  private static class ConfirmHandler implements SplibOauth2UserHandlerWithRegistrationConfirm {
    private final @Nullable UserDetails foundUser;
    private final boolean throwsRejected;
    private final List<String> calls = new ArrayList<>();

    ConfirmHandler(@Nullable UserDetails foundUser) {
      this(foundUser, false);
    }

    ConfirmHandler(@Nullable UserDetails foundUser, boolean throwsRejected) {
      this.foundUser = foundUser;
      this.throwsRejected = throwsRejected;
    }

    @Override
    public @Nullable UserDetails findUser(SplibOauth2UserInfo userInfo) {
      calls.add("findUser:" + userInfo);
      if (throwsRejected) {
        throw new SplibOauth2LoginRejectedException("rejected by the app.");
      }
      return foundUser;
    }

    @Override
    public String getRegistrationConfirmPage() {
      return CONFIRM_PAGE;
    }

    @Override
    public void afterLoginSuccess(HttpServletRequest request, UserDetails userDetails) {
      calls.add("afterLoginSuccess:" + userDetails.getUsername());
    }
  }

  @Nested
  @DisplayName("with SplibOauth2UserHandler")
  class WithFindOrCreate {

    @Test
    @DisplayName("logs in the user returned by findOrCreateUser")
    void logsIn() throws Exception {
      List<String> calls = new ArrayList<>();
      SplibOauth2UserHandler userHandler = userInfo -> {
        calls.add(userInfo.toString());
        return userDetails();
      };
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(
          oauth2CallbackRequest(token("google", "Taro")), response, token("google", "Taro"));

      assertThat(calls).containsExactly(
          new SplibOauth2UserInfo("a@example.com", "Taro", "GOOGLE", "sub1").toString());
      assertThat(loggedInPrincipal()).isEqualTo(userDetails());
      assertThat(response.getRedirectedUrl()).isEqualTo(DEFAULT_URL);
    }

    @Test
    @DisplayName("logs in when only the credentials have expired, which are not used in OAuth2")
    void credentialsExpired() throws Exception {
      UserDetails user = User.withUserDetails(userDetails()).credentialsExpired(true).build();
      FindOrCreateHandler userHandler = new FindOrCreateHandler(user);
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(
          oauth2CallbackRequest(token("google", "Taro")), response, token("google", "Taro"));

      assertThat(userHandler.calls).contains("afterLoginSuccess:a@example.com");
      assertThat(loggedInPrincipal()).isEqualTo(user);
      assertThat(response.getRedirectedUrl()).isEqualTo(DEFAULT_URL);
    }

    @ParameterizedTest
    @MethodSource("jp.ecuacion.splib.web.oauth2.SplibOauth2AuthSuccessHandlerTest"
        + "#rejectedUserDetails")
    @DisplayName("rejects the login when the account status doesn't allow it")
    void rejectedByAccountStatus(UserDetails user, String reason) throws Exception {
      FindOrCreateHandler userHandler = new FindOrCreateHandler(user);
      MockHttpServletRequest request = oauth2CallbackRequest(token("google", "Taro"));
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(request, response, token("google", "Taro"));

      assertRejected(request, response.getRedirectedUrl(), reason);
      assertThat(userHandler.calls).noneMatch(call -> call.startsWith("afterLoginSuccess"));
      assertThat(savedAuthorizedClient(request, token("google", "Taro"))).isNull();
    }

    @Test
    @DisplayName("rejects the login when SplibOauth2LoginRejectedException is thrown")
    void rejectedByException() throws Exception {
      FindOrCreateHandler userHandler = new FindOrCreateHandler(null);
      MockHttpServletRequest request = oauth2CallbackRequest(token("google", "Taro"));
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(request, response, token("google", "Taro"));

      assertRejected(request, response.getRedirectedUrl(), "rejected by the app.");
      assertThat(userHandler.calls).noneMatch(call -> call.startsWith("afterLoginSuccess"));
      assertThat(savedAuthorizedClient(request, token("google", "Taro"))).isNull();
    }

    @Test
    @DisplayName("another account can log in after the rejection in the same session")
    void loginAfterRejection() throws Exception {
      MockHttpServletRequest rejectedRequest = oauth2CallbackRequest(token("google", "Taro"));
      handler(new FindOrCreateHandler(null)).onAuthenticationSuccess(rejectedRequest,
          new MockHttpServletResponse(), token("google", "Taro"));

      FindOrCreateHandler userHandler = new FindOrCreateHandler(userDetails());
      MockHttpServletResponse response = new MockHttpServletResponse();
      handler(userHandler).onAuthenticationSuccess(oauth2CallbackRequest(token("apple", "Jiro")),
          response, token("apple", "Jiro"));

      assertThat(userHandler.calls).contains("afterLoginSuccess:a@example.com");
      assertThat(loggedInPrincipal()).isEqualTo(userDetails());
      assertThat(response.getRedirectedUrl()).isEqualTo(DEFAULT_URL);
    }
  }

  @Nested
  @DisplayName("with SplibOauth2UserHandlerWithRegistrationConfirm")
  class WithRegistrationConfirm {

    @Test
    @DisplayName("logs in the found user and discards a stale pending registration")
    void userFound() throws Exception {
      ConfirmHandler userHandler = new ConfirmHandler(userDetails());
      MockHttpServletRequest request = oauth2CallbackRequest(token("google", "Taro"));
      session.setAttribute(SplibOauth2PendingRegistration.SESSION_KEY,
          new SplibOauth2PendingRegistration(
              new SplibOauth2UserInfo("old@example.com", "old", "APPLE", "sub0")));
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(request, response, token("google", "Taro"));

      assertThat(userHandler.calls).containsExactly(
          "findUser:" + new SplibOauth2UserInfo("a@example.com", "Taro", "GOOGLE", "sub1"),
          "afterLoginSuccess:a@example.com");
      assertThat(loggedInPrincipal()).isEqualTo(userDetails());
      assertThat(session.getAttribute(SplibOauth2PendingRegistration.SESSION_KEY)).isNull();
      assertThat(response.getRedirectedUrl()).isEqualTo(DEFAULT_URL);
    }

    @Test
    @DisplayName("redirects to the confirm page without logging in when the user is not found")
    void userNotFound() throws Exception {
      ConfirmHandler userHandler = new ConfirmHandler(null);
      MockHttpServletRequest request = oauth2CallbackRequest(token("apple", null));
      request.setParameter("user", "{\"name\":{\"firstName\":\"Hanako\",\"lastName\":\"Sato\"}}");
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(request, response, token("apple", null));

      SplibOauth2UserInfo userInfo =
          new SplibOauth2UserInfo("a@example.com", "Hanako Sato", "APPLE", "sub1");
      assertThat(userHandler.calls).containsExactly("findUser:" + userInfo);
      assertThat(session.getAttribute(CONTEXT_KEY)).isNull();
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      assertThat(savedAuthorizedClient(request, token("apple", null))).isNull();
      assertThat(new SplibOauth2RegistrationConfirmService(handler(userHandler))
          .getPendingRegistration(request))
              .isEqualTo(new SplibOauth2PendingRegistration(userInfo));
      assertThat(response.getRedirectedUrl()).isEqualTo(CONFIRM_PAGE);
    }

    @ParameterizedTest
    @MethodSource("jp.ecuacion.splib.web.oauth2.SplibOauth2AuthSuccessHandlerTest"
        + "#rejectedUserDetails")
    @DisplayName("rejects the login when the account status of the found user doesn't allow it")
    void rejectedByAccountStatus(UserDetails user, String reason) throws Exception {
      ConfirmHandler userHandler = new ConfirmHandler(user);
      MockHttpServletRequest request = oauth2CallbackRequest(token("google", "Taro"));
      session.setAttribute(SplibOauth2PendingRegistration.SESSION_KEY,
          new SplibOauth2PendingRegistration(
              new SplibOauth2UserInfo("old@example.com", "old", "APPLE", "sub0")));
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(request, response, token("google", "Taro"));

      assertRejected(request, response.getRedirectedUrl(), reason);
      assertThat(userHandler.calls).noneMatch(call -> call.startsWith("afterLoginSuccess"));
      assertThat(savedAuthorizedClient(request, token("google", "Taro"))).isNull();
    }

    @Test
    @DisplayName("rejects the login when findUser throws SplibOauth2LoginRejectedException")
    void rejectedByException() throws Exception {
      ConfirmHandler userHandler = new ConfirmHandler(null, true);
      MockHttpServletRequest request = oauth2CallbackRequest(token("google", "Taro"));
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(request, response, token("google", "Taro"));

      assertRejected(request, response.getRedirectedUrl(), "rejected by the app.");
      assertThat(userHandler.calls).noneMatch(call -> call.startsWith("afterLoginSuccess"));
    }
  }

  @Nested
  @DisplayName("SplibOauth2RegistrationConfirmService")
  class Service {

    private MockHttpServletRequest confirmRequest() {
      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/public/registrationConfirm/action");
      request.setSession(session);
      // As if the session cookie is sent (false by default after setSession())
      request.setRequestedSessionIdValid(true);
      session.setAttribute(SplibOauth2PendingRegistration.SESSION_KEY,
          new SplibOauth2PendingRegistration(
              new SplibOauth2UserInfo("a@example.com", "Taro", "GOOGLE", "sub1")));
      return request;
    }

    @Test
    @DisplayName("loginAfterRegistration logs in with a new session id"
        + " and returns the default url")
    void loginAfterRegistration() {
      ConfirmHandler userHandler = new ConfirmHandler(null);
      SplibOauth2RegistrationConfirmService service =
          new SplibOauth2RegistrationConfirmService(handler(userHandler));
      MockHttpServletRequest request = confirmRequest();
      String sessionIdBefore = session.getId();

      String url =
          service.loginAfterRegistration(request, new MockHttpServletResponse(), userDetails());

      assertThat(url).isEqualTo(DEFAULT_URL);
      assertThat(session.getId()).isNotEqualTo(sessionIdBefore);
      assertThat(loggedInPrincipal()).isEqualTo(userDetails());
      assertThat(service.getPendingRegistration(request)).isNull();
      assertThat(userHandler.calls).containsExactly("afterLoginSuccess:a@example.com");
    }

    @Test
    @DisplayName("loginAfterRegistration returns the url saved before login")
    void returnsSavedRequestUrl() {
      MockHttpServletRequest originalRequest =
          new MockHttpServletRequest("GET", "/account/inquiry/detail/page");
      originalRequest.setQueryString("id=1");
      originalRequest.setSession(session);
      new HttpSessionRequestCache().saveRequest(originalRequest, new MockHttpServletResponse());
      SplibOauth2RegistrationConfirmService service =
          new SplibOauth2RegistrationConfirmService(handler(new ConfirmHandler(null)));

      String url = service.loginAfterRegistration(confirmRequest(), new MockHttpServletResponse(),
          userDetails());

      assertThat(url).contains("/account/inquiry/detail/page?id=1");
    }

    @ParameterizedTest
    @MethodSource("jp.ecuacion.splib.web.oauth2.SplibOauth2AuthSuccessHandlerTest"
        + "#rejectedUserDetails")
    @DisplayName("loginAfterRegistration rejects the login"
        + " when the account status doesn't allow it")
    void loginAfterRegistrationRejected(UserDetails user, String reason) {
      ConfirmHandler userHandler = new ConfirmHandler(null);
      SplibOauth2RegistrationConfirmService service =
          new SplibOauth2RegistrationConfirmService(handler(userHandler));
      MockHttpServletRequest request = confirmRequest();

      String url = service.loginAfterRegistration(request, new MockHttpServletResponse(), user);

      assertRejected(request, url, reason);
      assertThat(userHandler.calls).isEmpty();
    }

    @Test
    @DisplayName("rejectLogin discards the info without logging in"
        + " and returns the login page with ?oauth2Rejected")
    void rejectLogin() {
      ConfirmHandler userHandler = new ConfirmHandler(null);
      SplibOauth2RegistrationConfirmService service =
          new SplibOauth2RegistrationConfirmService(handler(userHandler));
      MockHttpServletRequest request = confirmRequest();

      String url =
          service.rejectLogin(request, new MockHttpServletResponse(), "cannot be registered.");

      assertRejected(request, url, "cannot be registered.");
      assertThat(userHandler.calls).isEmpty();
    }

    @Test
    @DisplayName("discardPendingRegistration discards the info without logging in")
    void discard() {
      SplibOauth2RegistrationConfirmService service =
          new SplibOauth2RegistrationConfirmService(handler(new ConfirmHandler(null)));
      MockHttpServletRequest request = confirmRequest();

      service.discardPendingRegistration(request);

      assertThat(service.getPendingRegistration(request)).isNull();
      assertThat(loggedInPrincipal()).isNull();
    }
  }
}
