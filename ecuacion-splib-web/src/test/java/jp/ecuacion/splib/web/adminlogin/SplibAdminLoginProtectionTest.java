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
package jp.ecuacion.splib.web.adminlogin;

import static jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection.PROPERTY_ATTEMPT_LIMIT_ENABLED;
import static jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection.PROPERTY_LOGIN_CODE_ENABLED;
import static jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection.PROPERTY_SESSION_CHECK_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.splib.web.adminlogin.AdminLoginTestSupport.CapturingMailUtil;
import jp.ecuacion.splib.web.adminlogin.AdminLoginTestSupport.MutableClock;
import jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection.LoginCodeState;
import jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection.ResendResult;
import jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection.VerifyResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * Tests the login code, the session check and the attempt limit of the admin login.
 *
 * <p>The scenarios of {@code inquiry} (e2e A-20, A-21, A-24 to A-27) are tested here and in
 *     {@code SplibWebSecurityConfigForAdminLoginProtectionTest} (the filter chain).</p>
 */
@DisplayName("SplibAdminLoginProtection")
class SplibAdminLoginProtectionTest {

  private static final String ADMIN = "admin@example.com";

  private final MutableClock clock = new MutableClock();
  private final CapturingMailUtil mail = new CapturingMailUtil();
  private final Map<String, SplibAdminAccountState> accounts = new HashMap<>();

  private SplibAdminLoginProtection protection = create(true, true, true);
  private final MockHttpSession session = new MockHttpSession();

  @BeforeEach
  void setUp() {
    accounts.put(ADMIN, new SplibAdminAccountState(ADMIN, "admin@example.com\nADMIN\nhash1"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private SplibAdminLoginProtection create(boolean loginCode, boolean sessionCheck,
      boolean attemptLimit) {
    MockEnvironment env = new MockEnvironment()
        .withProperty(PROPERTY_LOGIN_CODE_ENABLED, String.valueOf(loginCode))
        .withProperty(PROPERTY_SESSION_CHECK_ENABLED, String.valueOf(sessionCheck))
        .withProperty(PROPERTY_ATTEMPT_LIMIT_ENABLED, String.valueOf(attemptLimit));
    return new SplibAdminLoginProtection(env, accounts::get, mail, "/admin/home/page", clock);
  }

  private MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    return request;
  }

  /** Logs in by the password, as the admin login page does. */
  private MockHttpServletResponse login() throws Exception {
    Authentication auth = UsernamePasswordAuthenticationToken.authenticated(ADMIN, null,
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN_FULL_ACCESS"),
            FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)));
    SecurityContextHolder.getContext().setAuthentication(auth);
    MockHttpServletResponse response = new MockHttpServletResponse();
    protection.createLoginSuccessHandler().onAuthenticationSuccess(request(), response, auth);
    return response;
  }

  private VerifyResult verify(String code) {
    return protection.verifyLoginCode(request(), new MockHttpServletResponse(), code);
  }

  private static String wrongCodeOf(String code) {
    return code.equals("000000") ? "111111" : "000000";
  }

  @Nested
  @DisplayName("login code")
  class LoginCode {

    @Test
    @DisplayName("login sends a code to the account's address and moves to the page to enter it")
    void loginSendsCode() throws Exception {
      MockHttpServletResponse response = login();

      assertThat(response.getRedirectedUrl())
          .isEqualTo(SplibAdminLoginProtection.LOGIN_CODE_PAGE_PATH);
      assertThat(mail.addresses).containsExactly(ADMIN);
      assertThat(mail.lastCode()).hasSize(6);
      assertThat(protection.getLoginCodeState(request())).isEqualTo(LoginCodeState.PENDING);
    }

    @Test
    @DisplayName("the right code adds FACTOR_OTT, changes the session ID and saves the context")
    void rightCode() throws Exception {
      login();
      String sessionIdBefore = session.getId();

      assertThat(verify(mail.lastCode())).isEqualTo(VerifyResult.OK);

      Authentication auth =
          Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication());
      assertThat(auth.getAuthorities()).extracting(a -> a.getAuthority())
          .contains(FactorGrantedAuthority.OTT_AUTHORITY, "ROLE_ADMIN_FULL_ACCESS");
      assertThat(session.getId()).isNotEqualTo(sessionIdBefore);
      assertThat(session.getAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
      assertThat(protection.getLoginCodeState(request())).isEqualTo(LoginCodeState.VERIFIED);
      // the code cannot be used twice
      assertThat(verify(mail.lastCode())).isEqualTo(VerifyResult.NOT_PENDING);
    }

    @Test
    @DisplayName("A-25: 5 wrong codes make the code unusable, even the right one after them")
    void fiveWrongCodes() throws Exception {
      login();
      String code = mail.lastCode();
      for (int i = 0; i < 4; i++) {
        assertThat(verify(wrongCodeOf(code))).isEqualTo(VerifyResult.WRONG);
      }

      assertThat(verify(wrongCodeOf(code))).isEqualTo(VerifyResult.UNUSABLE);
      assertThat(verify(code)).isEqualTo(VerifyResult.UNUSABLE);
    }

    @Test
    @DisplayName("A-25: the resent code can be used, and the old one cannot")
    void resend() throws Exception {
      login();
      String oldCode = mail.lastCode();

      assertThat(protection.resendLoginCode(request())).isEqualTo(ResendResult.SENT);
      String newCode = mail.lastCode();
      if (!oldCode.equals(newCode)) {
        assertThat(verify(oldCode)).isEqualTo(VerifyResult.WRONG);
      }

      assertThat(verify(newCode)).isEqualTo(VerifyResult.OK);
    }

    @Test
    @DisplayName("A-27: the code expires in 10 minutes")
    void expires() throws Exception {
      login();
      clock.advance(SplibAdminLoginProtection.LOGIN_CODE_VALID_DURATION.plusSeconds(1));

      assertThat(verify(mail.lastCode())).isEqualTo(VerifyResult.UNUSABLE);
    }

    @Test
    @DisplayName("wrong codes are counted per login ID across logins (10 times in 60 minutes)")
    void wrongCodesAcrossLogins() throws Exception {
      login();
      for (int i = 0; i < 5; i++) {
        verify(wrongCodeOf(mail.lastCode()));
      }

      // logging in again issues a new code, but the count is kept
      login();
      for (int i = 0; i < 4; i++) {
        assertThat(verify(wrongCodeOf(mail.lastCode()))).isEqualTo(VerifyResult.WRONG);
      }

      assertThat(verify(wrongCodeOf(mail.lastCode())))
          .isEqualTo(VerifyResult.TOO_MANY_FAILURES);
      login();
      assertThat(verify(mail.lastCode())).isEqualTo(VerifyResult.TOO_MANY_FAILURES);

      clock.advance(Duration.ofMinutes(60));
      login();
      assertThat(verify(mail.lastCode())).isEqualTo(VerifyResult.OK);
    }

    @Test
    @DisplayName("resending is limited to 5 times in 60 minutes")
    void resendLimit() throws Exception {
      login();
      for (int i = 0; i < 5; i++) {
        assertThat(protection.resendLoginCode(request())).isEqualTo(ResendResult.SENT);
      }

      assertThat(protection.resendLoginCode(request())).isEqualTo(ResendResult.LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("failing to send the mail does not stop the login, and resend reports it")
    void mailFailure() throws Exception {
      mail.fails = true;
      MockHttpServletResponse response = login();

      assertThat(response.getRedirectedUrl())
          .isEqualTo(SplibAdminLoginProtection.LOGIN_CODE_PAGE_PATH);
      assertThat(protection.resendLoginCode(request())).isEqualTo(ResendResult.SEND_FAILED);
    }

    @Test
    @DisplayName("A-26: without the admin login, the state is NOT_LOGGED_IN and nothing is done")
    void notLoggedIn() {
      assertThat(protection.getLoginCodeState(request())).isEqualTo(LoginCodeState.NOT_LOGGED_IN);
      assertThat(verify("123456")).isEqualTo(VerifyResult.NOT_PENDING);
      assertThat(protection.resendLoginCode(request())).isEqualTo(ResendResult.NOT_PENDING);
    }

    @Test
    @DisplayName("an account which cannot log in anymore cannot enter the code")
    void accountUnavailable() throws Exception {
      login();
      accounts.remove(ADMIN);

      assertThat(verify(mail.lastCode())).isEqualTo(VerifyResult.ACCOUNT_UNAVAILABLE);
      assertThat(protection.resendLoginCode(request()))
          .isEqualTo(ResendResult.ACCOUNT_UNAVAILABLE);
    }

    @Test
    @DisplayName("without the login code, login moves to the default success url")
    void disabled() throws Exception {
      protection = create(false, true, true);
      MockHttpServletResponse response = login();

      assertThat(response.getRedirectedUrl()).isEqualTo("/admin/home/page");
      assertThat(mail.bodies).isEmpty();
    }
  }

  @Nested
  @DisplayName("session check")
  class SessionCheck {

    @Test
    @DisplayName("the session is valid after the login")
    void validAfterLogin() throws Exception {
      login();
      assertThat(protection.isSessionValid(request(), ADMIN)).isTrue();
    }

    @Test
    @DisplayName("A-21: the session is invalid when the stamp has changed or the account is gone")
    void invalidWhenChanged() throws Exception {
      login();
      accounts.put(ADMIN, new SplibAdminAccountState(ADMIN, "admin@example.com\nADMIN\nhash2"));
      assertThat(protection.isSessionValid(request(), ADMIN)).isFalse();

      accounts.remove(ADMIN);
      assertThat(protection.isSessionValid(request(), ADMIN)).isFalse();
    }

    @Test
    @DisplayName("A-21: recordSessionStamp keeps the session of the admin who changed it")
    void recordSessionStamp() throws Exception {
      login();
      verify(mail.lastCode());
      accounts.put(ADMIN, new SplibAdminAccountState(ADMIN, "admin@example.com\nADMIN\nhash2"));

      protection.recordSessionStamp(request());
      assertThat(protection.isSessionValid(request(), ADMIN)).isTrue();
    }

    @Test
    @DisplayName("the session of another login ID is invalid")
    void anotherLoginId() throws Exception {
      login();
      assertThat(protection.isSessionValid(request(), "other@example.com")).isFalse();
    }
  }

  @Nested
  @DisplayName("attempt limit")
  class AttemptLimit {

    private void fail(AuthenticationFailureHandler handler, String username) throws Exception {
      MockHttpServletRequest request = request();
      request.addParameter(SplibAdminLoginProtection.USERNAME_PARAMETER, username);
      handler.onAuthenticationFailure(request, new MockHttpServletResponse(),
          new BadCredentialsException("wrong"));
    }

    private void authenticate() {
      protection.createAttemptLimitAuthenticationProvider()
          .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(ADMIN, "password"));
    }

    @Test
    @DisplayName("A-20: 10 wrong passwords lock the login ID, without checking the password")
    void locked() throws Exception {
      AuthenticationFailureHandler handler =
          protection.createLoginFailureHandler("/public/adminLogin/page?error");
      for (int i = 0; i < 9; i++) {
        fail(handler, " " + ADMIN + " ");
      }

      authenticate();
      fail(handler, ADMIN);
      assertThatThrownBy(this::authenticate).isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("A-20: resetPasswordFailures (a password set by mail) unlocks the login ID")
    void reset() throws Exception {
      AuthenticationFailureHandler handler =
          protection.createLoginFailureHandler("/public/adminLogin/page?error");
      for (int i = 0; i < 10; i++) {
        fail(handler, ADMIN);
      }

      protection.resetPasswordFailures(ADMIN);
      authenticate();
    }

    @Test
    @DisplayName("the login removes the count of wrong passwords")
    void loginResets() throws Exception {
      AuthenticationFailureHandler handler =
          protection.createLoginFailureHandler("/public/adminLogin/page?error");
      for (int i = 0; i < 9; i++) {
        fail(handler, ADMIN);
      }

      login();
      fail(handler, ADMIN);
      authenticate();
    }

    @Test
    @DisplayName("the failure handler redirects to the failure url")
    void redirect() throws Exception {
      MockHttpServletResponse response = new MockHttpServletResponse();
      protection.createLoginFailureHandler("/public/adminLogin/page?error")
          .onAuthenticationFailure(request(), response, new BadCredentialsException("wrong"));
      assertThat(response.getRedirectedUrl()).isEqualTo("/public/adminLogin/page?error");
    }

    @Test
    @DisplayName("without the attempt limit, wrong passwords are not counted")
    void disabled() throws Exception {
      protection = create(false, false, false);
      AuthenticationFailureHandler handler =
          protection.createLoginFailureHandler("/public/adminLogin/page?error");
      for (int i = 0; i < 20; i++) {
        fail(handler, ADMIN);
      }

      authenticate();
    }
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("the login code and the session check need SplibAdminAccountProvider")
    void needsProvider() {
      MockEnvironment env = new MockEnvironment().withProperty(PROPERTY_SESSION_CHECK_ENABLED,
          "true");
      assertThatThrownBy(() -> new SplibAdminLoginProtection(env, null, mail, "/admin/home/page"))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("the login code needs SplibMailUtil")
    void needsMailUtil() {
      MockEnvironment env =
          new MockEnvironment().withProperty(PROPERTY_LOGIN_CODE_ENABLED, "true");
      assertThatThrownBy(
          () -> new SplibAdminLoginProtection(env, accounts::get, null, "/admin/home/page"))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("nothing is enabled by default, and nothing is needed")
    void disabledByDefault() {
      SplibAdminLoginProtection p =
          new SplibAdminLoginProtection(new MockEnvironment(), null, null, "/admin/home/page");
      assertThat(p.isAnyEnabled()).isFalse();
    }
  }
}
