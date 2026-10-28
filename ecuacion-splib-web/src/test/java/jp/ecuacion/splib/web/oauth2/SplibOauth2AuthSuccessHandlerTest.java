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
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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

  private MockHttpSession session = new MockHttpSession();

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Simulates the state right after Spring Security's OAuth2 login filter has run. */
  private MockHttpServletRequest oauth2CallbackRequest(OAuth2AuthenticationToken token) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login/oauth2/code/x");
    request.setSession(session);
    SecurityContext context = new SecurityContextImpl(token);
    SecurityContextHolder.setContext(context);
    session.setAttribute(CONTEXT_KEY, context);
    return request;
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
  private static class ConfirmHandler implements SplibOauth2UserHandlerWithRegistrationConfirm {
    private final @Nullable UserDetails foundUser;
    private final List<String> calls = new ArrayList<>();

    ConfirmHandler(@Nullable UserDetails foundUser) {
      this.foundUser = foundUser;
    }

    @Override
    public @Nullable UserDetails findUser(String email, String name, String provider) {
      calls.add("findUser:" + email + "," + name + "," + provider);
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
      SplibOauth2UserHandler userHandler = (email, name, provider) -> {
        calls.add(email + "," + name + "," + provider);
        return userDetails();
      };
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(
          oauth2CallbackRequest(token("google", "Taro")), response, token("google", "Taro"));

      assertThat(calls).containsExactly("a@example.com,Taro,GOOGLE");
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
          new SplibOauth2PendingRegistration("old@example.com", "old", "APPLE"));
      MockHttpServletResponse response = new MockHttpServletResponse();

      handler(userHandler).onAuthenticationSuccess(request, response, token("google", "Taro"));

      assertThat(userHandler.calls).containsExactly("findUser:a@example.com,Taro,GOOGLE",
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

      assertThat(userHandler.calls).containsExactly("findUser:a@example.com,Hanako Sato,APPLE");
      assertThat(session.getAttribute(CONTEXT_KEY)).isNull();
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
      assertThat(new SplibOauth2RegistrationConfirmService(handler(userHandler))
          .getPendingRegistration(request)).isEqualTo(
              new SplibOauth2PendingRegistration("a@example.com", "Hanako Sato", "APPLE"));
      assertThat(response.getRedirectedUrl()).isEqualTo(CONFIRM_PAGE);
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
          new SplibOauth2PendingRegistration("a@example.com", "Taro", "GOOGLE"));
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
