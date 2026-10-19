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
package jp.ecuacion.splib.rest.exceptionhandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.Violations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies that {@link SplibRestExceptionHandler} coexists with an unscoped, unordered
 * {@code @ControllerAdvice} — the shape of an app's {@code SplibWebExceptionHandler} subclass
 * when {@code ecuacion-splib-web} is used in the same application.
 *
 * <p>Each case registers the two advices in the order that would make it fail if
 *     {@link SplibRestExceptionHandler} relied on registration order rather than its own scope
 *     and {@code @Order}.</p>
 */
@DisplayName("SplibRestExceptionHandler coexisting with a web-side ControllerAdvice")
class SplibRestExceptionHandlerCoexistenceTest {

  private static final String MESSAGE_ID =
      "jp.ecuacion.lib.core.util.EmbeddedVariableUtil.paramNotFoundInMap.message";

  private static final String WEB_HANDLED = "handled-by-web-advice";

  @RestController
  static class ApiController {

    @GetMapping("/api/violation")
    String violation() {
      throw new ViolationException(new Violations().add(MESSAGE_ID, "MY_VAR"));
    }

    @GetMapping("/api/error")
    String error() {
      throw new IllegalStateException("unexpected");
    }
  }

  @Controller
  static class PageController {

    @GetMapping("/page/violation")
    String violation() {
      throw new ViolationException(new Violations().add(MESSAGE_ID, "MY_VAR"));
    }

    @GetMapping("/page/error")
    String error() {
      throw new IllegalStateException("unexpected");
    }
  }

  /** Stands in for an app's {@code SplibWebExceptionHandler} subclass. */
  @ControllerAdvice
  static class WebSideAdvice {

    @SuppressWarnings("null")
    @ExceptionHandler({ViolationException.class, Throwable.class})
    ResponseEntity<String> handle(Throwable exception) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .contentType(MediaType.TEXT_PLAIN).body(WEB_HANDLED);
    }
  }

  private MockMvc mockMvc(Object... advices) {
    return MockMvcBuilders.standaloneSetup(new ApiController(), new PageController())
        .setControllerAdvice(advices).build();
  }

  @Nested
  @DisplayName("@RestController endpoints")
  class RestEndpoints {

    // Web-side advice registered first: it would win if order were left to registration.
    private final MockMvc mockMvc =
        mockMvc(new WebSideAdvice(), new SplibRestExceptionHandler(null, null));

    @Test
    @DisplayName("ViolationException is answered by SplibRestExceptionHandler as 400 JSON")
    void violationHandledByRestHandler() throws Exception {
      mockMvc.perform(get("/api/violation")).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.messages[0]").exists());
    }

    @Test
    @DisplayName("an unexpected exception is answered by SplibRestExceptionHandler as 500")
    void throwableHandledByRestHandler() throws Exception {
      mockMvc.perform(get("/api/error")).andExpect(status().isInternalServerError());
    }
  }

  @Nested
  @DisplayName("@Controller (page) endpoints")
  class PageEndpoints {

    // SplibRestExceptionHandler registered first: it would win if it weren't scoped.
    private final MockMvc mockMvc =
        mockMvc(new SplibRestExceptionHandler(null, null), new WebSideAdvice());

    @Test
    @DisplayName("ViolationException is left to the web-side advice")
    void violationLeftToWebAdvice() throws Exception {
      mockMvc.perform(get("/page/violation")).andExpect(status().isServiceUnavailable())
          .andExpect(content().string(WEB_HANDLED));
    }

    @Test
    @DisplayName("an unexpected exception is left to the web-side advice")
    void throwableLeftToWebAdvice() throws Exception {
      mockMvc.perform(get("/page/error")).andExpect(status().isServiceUnavailable())
          .andExpect(content().string(WEB_HANDLED));
    }
  }
}
