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
import java.net.ConnectException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

@DisplayName("SplibOauth2AuthFailureHandler")
class SplibOauth2AuthFailureHandlerTest {

  private static final String LOGIN_PAGE = "/public/login/page";

  private final Logger logger =
      (Logger) LoggerFactory.getLogger(SplibOauth2AuthFailureHandler.class.getName());
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void setUp() {
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.INFO);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    logger.setLevel(null);
  }

  private @Nullable String handle(AuthenticationException exception) throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/app/login/oauth2/code/google");
    request.setContextPath("/app");
    MockHttpServletResponse response = new MockHttpServletResponse();
    new SplibOauth2AuthFailureHandler(LOGIN_PAGE).onAuthenticationFailure(request, response,
        exception);
    return response.getRedirectedUrl();
  }

  private static OAuth2AuthenticationException oauth2Exception(String errorCode) {
    return new OAuth2AuthenticationException(new OAuth2Error(errorCode, "desc", null));
  }

  private ILoggingEvent onlyLog() {
    assertThat(appender.list).hasSize(1);
    return appender.list.get(0);
  }

  @ParameterizedTest(name = "{0} -> {1}, {2}")
  @CsvSource({"access_denied, /app/public/login/page, INFO",
      "user_cancelled_authorize, /app/public/login/page, INFO",
      "authorization_request_not_found, /app/public/login/page?oauth2Error, INFO",
      "invalid_token_response, /app/public/login/page?oauth2Error, WARN",
      "invalid_client, /app/public/login/page?oauth2Error, WARN",
      "invalid_id_token, /app/public/login/page?oauth2Error, WARN",
      "unknown_code, /app/public/login/page?oauth2Error, WARN"})
  @DisplayName("the redirect and the log level are decided by the error code")
  void redirectAndLogLevel(String errorCode, String expectedUrl, String expectedLevel)
      throws Exception {
    assertThat(handle(oauth2Exception(errorCode))).isEqualTo(expectedUrl);
    assertThat(onlyLog().getLevel()).isEqualTo(Level.toLevel(expectedLevel));
  }

  @Test
  @DisplayName("the log has the registrationId, the error code and the description")
  void logContents() throws Exception {
    handle(oauth2Exception("invalid_client"));

    assertThat(onlyLog().getFormattedMessage()).contains("registrationId: google")
        .contains("errorCode: invalid_client").contains("description: desc")
        .doesNotContain("cause:");
  }

  @Test
  @DisplayName("the log has the class and the message of the root cause")
  void logRootCause() throws Exception {
    OAuth2Error error = new OAuth2Error("invalid_token_response");
    handle(new OAuth2AuthenticationException(error, "message",
        new RuntimeException("wrapper", new ConnectException("refused"))));

    assertThat(onlyLog().getFormattedMessage())
        .contains("cause: java.net.ConnectException: refused");
  }

  @Test
  @DisplayName("long values from the callback are shortened in the log")
  void longValueShortened() throws Exception {
    handle(oauth2Exception("x".repeat(10000)));

    assertThat(onlyLog().getFormattedMessage().length()).isLessThan(1000);
  }

  @Test
  @DisplayName("an exception other than OAuth2's is logged with WARN and shows the message")
  void otherException() throws Exception {
    assertThat(handle(new BadCredentialsException("bad")))
        .isEqualTo("/app/public/login/page?oauth2Error");

    assertThat(onlyLog().getLevel()).isEqualTo(Level.WARN);
    assertThat(onlyLog().getFormattedMessage()).contains("errorCode: null")
        .contains("description: bad");
  }
}
