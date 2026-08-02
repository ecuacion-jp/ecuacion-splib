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
package jp.ecuacion.splib.rest.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.rest.dto.ViolationsResponse;
import jp.ecuacion.splib.rest.exceptionhandler.SplibRestExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Verifies {@link SplibRestExceptionHandler#handleViolationException} — the branch of exception
 * handling meant for a human end user (see the class Javadoc): every violation must come back
 * localized and included, unlike the developer-facing
 * {@link org.springframework.web.server.ResponseStatusException} path.
 */
@DisplayName("SplibRestExceptionHandler")
class SplibRestExceptionHandlerTest {

  private static final String MESSAGE_ID =
      "jp.ecuacion.lib.core.util.EmbeddedVariableUtil.paramNotFoundInMap.message";

  private final SplibRestExceptionHandler handler = new SplibRestExceptionHandler(null);

  private ServletWebRequest requestWithLocale(Locale locale) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addPreferredLocale(locale);
    return new ServletWebRequest(request);
  }

  @Test
  @DisplayName("always responds 400 Bad Request, regardless of how many violations are present")
  void respondsBadRequest() {
    ViolationException exception =
        new ViolationException(new Violations().add(MESSAGE_ID, "MY_VAR"));

    ResponseEntity<ViolationsResponse> response =
        handler.handleViolationException(exception, requestWithLocale(Locale.ENGLISH));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("includes every violation's message, not just the first")
  void includesEveryViolation() {
    Violations violations =
        new Violations().add(MESSAGE_ID, "FIRST_VAR").add(MESSAGE_ID, "SECOND_VAR");
    ViolationException exception = new ViolationException(violations);

    ResponseEntity<ViolationsResponse> response =
        handler.handleViolationException(exception, requestWithLocale(Locale.ENGLISH));

    ViolationsResponse body = Objects.requireNonNull(response.getBody());
    assertThat(body.messages()).hasSize(2);
    assertThat(body.messages().get(0)).contains("FIRST_VAR");
    assertThat(body.messages().get(1)).contains("SECOND_VAR");
  }

  @Test
  @DisplayName("localizes the message to the request's locale")
  void localizesToRequestLocale() {
    ViolationException exception =
        new ViolationException(new Violations().add(MESSAGE_ID, "MY_VAR"));

    ResponseEntity<ViolationsResponse> enResponse =
        handler.handleViolationException(exception, requestWithLocale(Locale.ENGLISH));
    ResponseEntity<ViolationsResponse> jaResponse =
        handler.handleViolationException(exception, requestWithLocale(Locale.JAPANESE));

    String enMessage = Objects.requireNonNull(enResponse.getBody()).messages().get(0);
    String jaMessage = Objects.requireNonNull(jaResponse.getBody()).messages().get(0);
    assertThat(enMessage).isNotEqualTo(jaMessage);
  }
}
