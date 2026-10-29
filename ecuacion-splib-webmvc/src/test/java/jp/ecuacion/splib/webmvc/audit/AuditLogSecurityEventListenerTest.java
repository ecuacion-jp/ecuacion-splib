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
package jp.ecuacion.splib.webmvc.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import jp.ecuacion.splib.core.audit.AuditLogWriter;
import jp.ecuacion.splib.core.audit.AuditLogWriter.Entry;
import jp.ecuacion.splib.core.audit.enums.AuditLogEventType;
import jp.ecuacion.splib.core.audit.enums.AuditLogResult;
import jp.ecuacion.splib.webmvc.audit.AuditLogInterceptorTest.CapturingWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Tests for {@link AuditLogSecurityEventListener}. */
@DisplayName("AuditLogSecurityEventListener")
class AuditLogSecurityEventListenerTest {

  private final CapturingWriter writer = new CapturingWriter();

  private final AuditLogSecurityEventListener listener =
      new AuditLogSecurityEventListener(
          new AuditLogger(writer));

  @BeforeEach
  void setUp() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/public/login/action");
    request.setRemoteAddr("192.0.2.1");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static Authentication user(String name) {
    return UsernamePasswordAuthenticationToken.authenticated(
        User.withUsername(name).password("").roles("ACCOUNT").build(), null,
        List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT")));
  }

  private static UserDetails userDetails(String name) {
    return User.withUsername(name).password("").roles("ACCOUNT").build();
  }

  private Entry onlyRecord() {
    assertThat(writer.records).hasSize(1);
    return writer.records.get(0);
  }

  @Nested
  @DisplayName("login and logout")
  class LoginLogout {

    @Test
    @DisplayName("successful login: LOGIN / SUCCESS with the user and the request")
    void loginSuccess() {
      listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(user("user1")));

      Entry record = onlyRecord();
      assertThat(record.eventType()).isEqualTo(AuditLogEventType.LOGIN);
      assertThat(record.result()).isEqualTo(AuditLogResult.SUCCESS);
      assertThat(record.actor()).isEqualTo("user1");
      assertThat(record.details().get("clientIp")).isEqualTo("192.0.2.1");
      assertThat(record.details().get("uri")).isEqualTo("/public/login/action");
      assertThat(record.details().get("handler")).isNull();
    }

    @Test
    @DisplayName("failed login: LOGIN / FAILURE with the username as typed, "
        + "and without the password")
    void loginFailure() {
      listener.onAuthenticationFailure(new AuthenticationFailureBadCredentialsEvent(
          UsernamePasswordAuthenticationToken.unauthenticated("typed-name", "typed-password"),
          new BadCredentialsException("Bad credentials")));

      Entry record = onlyRecord();
      assertThat(record.eventType()).isEqualTo(AuditLogEventType.LOGIN);
      assertThat(record.result()).isEqualTo(AuditLogResult.FAILURE);
      assertThat(record.actor()).isEqualTo("typed-name");
      assertThat(record.error()).isEqualTo(BadCredentialsException.class.getName());
      assertThat(record.toString()).doesNotContain("typed-password");
    }

    @Test
    @DisplayName("logout: LOGOUT / SUCCESS with the user")
    void logout() {
      listener.onLogoutSuccess(new LogoutSuccessEvent(user("user1")));

      assertThat(onlyRecord().eventType()).isEqualTo(AuditLogEventType.LOGOUT);
      assertThat(onlyRecord().actor()).isEqualTo("user1");
    }

    @Test
    @DisplayName("outside a request: recorded without request items")
    void noRequest() {
      RequestContextHolder.resetRequestAttributes();

      listener.onLogoutSuccess(new LogoutSuccessEvent(user("user1")));

      assertThat(onlyRecord().details().get("clientIp")).isNull();
      assertThat(onlyRecord().details().get("uri")).isNull();
    }
  }

  @Nested
  @DisplayName("switch user")
  class SwitchUser {

    @Test
    @DisplayName("start: SWITCH_USER_START with the switched user as actor "
        + "and the administrator as originalActor")
    void start() {
      listener.onSwitchUser(new AuthenticationSwitchUserEvent(user("admin1"),
          userDetails("user1")));

      Entry record = onlyRecord();
      assertThat(record.eventType()).isEqualTo(AuditLogEventType.SWITCH_USER_START);
      assertThat(record.actor()).isEqualTo("user1");
      assertThat(record.originalActor()).isEqualTo("admin1");
    }

    @Test
    @DisplayName("exit: SWITCH_USER_EXIT with the switched user as actor "
        + "and the administrator as originalActor")
    void exit() {
      Authentication admin = user("admin1");
      Authentication switched = UsernamePasswordAuthenticationToken.authenticated("user1", null,
          List.of(new SwitchUserGrantedAuthority("ROLE_PREVIOUS_ADMINISTRATOR", admin)));

      listener.onSwitchUser(new AuthenticationSwitchUserEvent(switched, userDetails("admin1")));

      Entry record = onlyRecord();
      assertThat(record.eventType()).isEqualTo(AuditLogEventType.SWITCH_USER_EXIT);
      assertThat(record.actor()).isEqualTo("user1");
      assertThat(record.originalActor()).isEqualTo("admin1");
    }
  }

  @Test
  @DisplayName("an exception while writing does not reach the login")
  void writeExceptionDoesNotPropagate() {
    AuditLogWriter failingWriter = new AuditLogWriter() {
      @Override
      public void write(Entry record) {
        throw new IllegalStateException("writer");
      }
    };
    AuditLogSecurityEventListener failingWrite =
        new AuditLogSecurityEventListener(new AuditLogger(failingWriter));

    assertThatCode(
        () -> failingWrite.onAuthenticationSuccess(new AuthenticationSuccessEvent(user("user1"))))
        .doesNotThrowAnyException();
  }
}
