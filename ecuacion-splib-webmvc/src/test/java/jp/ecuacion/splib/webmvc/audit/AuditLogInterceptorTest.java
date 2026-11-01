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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.core.annotation.ReadOnlyOperation;
import jp.ecuacion.splib.core.audit.AuditLogConstants;
import jp.ecuacion.splib.core.audit.AuditLogContext;
import jp.ecuacion.splib.core.audit.AuditLogTarget;
import jp.ecuacion.splib.core.audit.AuditLogUtil;
import jp.ecuacion.splib.core.audit.AuditLogWriter;
import jp.ecuacion.splib.core.audit.AuditLogWriter.Entry;
import jp.ecuacion.splib.core.audit.AuditLogWriter.TargetData;
import jp.ecuacion.splib.core.audit.enums.AuditLogEventType;
import jp.ecuacion.splib.core.audit.enums.AuditLogResult;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Tests for {@link AuditLogInterceptor}, through a Spring MVC context with
 * {@link AuditLogConfig} enabled, so that the exception recorder and the interceptor are
 * registered the way applications get them.
 */
@DisplayName("AuditLogInterceptor")
@SpringJUnitWebConfig(AuditLogInterceptorTest.TestConfig.class)
@TestPropertySource(properties = AuditLogConstants.PROP_KEY_ENABLED + "=true")
class AuditLogInterceptorTest {

  /** Collects records instead of writing them. */
  static class CapturingWriter extends AuditLogWriter {
    final List<Entry> records = new ArrayList<>();

    @Override
    public void write(Entry record) {
      records.add(record);
    }
  }

  static class UnhandledException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  @Controller
  @RequestMapping("/account/sample")
  static class SampleController {

    /** What was being collected while the last request to ok() or get() was processed. */
    static @Nullable AuditLogContext seen;

    @PostMapping("ok")
    @ResponseBody
    String ok() {
      seen = AuditLogContext.current();
      AuditLogUtil.addTarget("Sample", 1L);
      AuditLogUtil.putAttribute("systemCode", "S1");
      // As ecuacion-splib-web's SplibControllerAdvice does for loginState.
      Objects.requireNonNull(AuditLogContext.current()).putDetail("loginState", "account");
      return "ok";
    }

    @PostMapping("violation")
    String violation() {
      throw new ViolationException(new Violations().add("MSG_ID_1").add("MSG_ID_2"));
    }

    @PostMapping("handledError")
    String handledError() {
      throw new IllegalStateException("unexpected");
    }

    @PostMapping("unhandledError")
    String unhandledError() {
      throw new UnhandledException();
    }

    @PostMapping("badRequest")
    ResponseEntity<String> badRequest() {
      return ResponseEntity.badRequest().body("bad");
    }

    @GetMapping("get")
    @ResponseBody
    String get() {
      seen = AuditLogContext.current();
      AuditLogUtil.addTarget("Sample", 1L);
      return "ok";
    }

    @AuditLogTarget
    @GetMapping("download")
    @ResponseBody
    String download() {
      return "ok";
    }

    @AuditLogTarget(false)
    @PostMapping("back")
    @ResponseBody
    String back() {
      return "ok";
    }

    @ReadOnlyOperation
    @PostMapping("showDetail")
    @ResponseBody
    String showDetail() {
      return "ok";
    }

    @AuditLogTarget
    @ReadOnlyOperation
    @PostMapping("showPersonalData")
    @ResponseBody
    String showPersonalData() {
      return "ok";
    }

    @ReadOnlyOperation(false)
    @GetMapping("getThatWrites")
    @ResponseBody
    String getThatWrites() {
      return "ok";
    }

    @PostMapping("async")
    @ResponseBody
    Callable<String> async() {
      AuditLogUtil.addTarget("Sample", 1L);
      return () -> "ok";
    }
  }

  /** Inherits {@code back()}'s annotation through the overridden method. */
  @Controller
  @RequestMapping("/account/sub")
  static class SubController extends SampleController {

    @Override
    @PostMapping("back")
    @ResponseBody
    String back() {
      return "ok";
    }
  }

  @Controller
  @RequestMapping("/account/notRecorded")
  @AuditLogTarget(false)
  static class NotRecordedController {

    @PostMapping("action")
    @ResponseBody
    String action() {
      return "ok";
    }
  }

  @ControllerAdvice
  static class Advice {

    /** Redirects back like {@code SplibWebExceptionHandler} does for a validation error. */
    @ExceptionHandler(ViolationException.class)
    String violation() {
      return "redirect:/account/sample/page";
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<String> error() {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
    }
  }

  /** {@link AuditLogConfig} with the writer replaced by {@link CapturingWriter}. */
  @Configuration
  static class TestAuditLogConfig extends AuditLogConfig {

    final CapturingWriter writer = new CapturingWriter();

    TestAuditLogConfig(ObjectProvider<AuditLogInterceptor> interceptor) {
      super(interceptor);
    }

    @Bean
    CapturingWriter capturingWriter() {
      return writer;
    }

    @Bean
    @Override
    AuditLogger splibAuditLogger() {
      return new AuditLogger(writer);
    }
  }

  @Configuration
  @EnableWebMvc
  @Import({TestAuditLogConfig.class, SampleController.class, SubController.class,
      NotRecordedController.class, Advice.class})
  static class TestConfig {
  }

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private CapturingWriter writer;

  @SuppressWarnings("null")
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    writer.records.clear();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static void login(Authentication authentication) {
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private static Authentication user(String name) {
    return UsernamePasswordAuthenticationToken.authenticated(name, null,
        List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT")));
  }

  private Entry onlyRecord() {
    assertThat(writer.records).hasSize(1);
    return writer.records.get(0);
  }

  @Nested
  @DisplayName("which requests are recorded")
  class WhichRequests {

    @Test
    @DisplayName("POST: recorded")
    void postIsRecorded() throws Exception {
      mockMvc.perform(post("/account/sample/ok")).andExpect(status().isOk());

      assertThat(writer.records).hasSize(1);
    }

    @Test
    @DisplayName("GET: not recorded, and AuditLogUtil.addTarget does nothing")
    void getIsNotRecorded() throws Exception {
      mockMvc.perform(get("/account/sample/get")).andExpect(status().isOk());

      assertThat(writer.records).isEmpty();
    }

    @Test
    @DisplayName("GET with @AuditLogTarget: recorded")
    void getWithAnnotation() throws Exception {
      mockMvc.perform(get("/account/sample/download")).andExpect(status().isOk());

      assertThat(onlyRecord().details().get("handler")).isEqualTo("SampleController#download");
    }

    @Test
    @DisplayName("POST with @AuditLogTarget(false): not recorded")
    void postWithAnnotationFalse() throws Exception {
      mockMvc.perform(post("/account/sample/back")).andExpect(status().isOk());

      assertThat(writer.records).isEmpty();
    }

    @Test
    @DisplayName("POST to an override of a @AuditLogTarget(false) method: not recorded")
    void inheritedFromOverriddenMethod() throws Exception {
      mockMvc.perform(post("/account/sub/back")).andExpect(status().isOk());

      assertThat(writer.records).isEmpty();
    }

    @Test
    @DisplayName("POST with @ReadOnlyOperation: not recorded")
    void postWithReadOnlyOperation() throws Exception {
      mockMvc.perform(post("/account/sample/showDetail")).andExpect(status().isOk());

      assertThat(writer.records).isEmpty();
    }

    @Test
    @DisplayName("POST with @ReadOnlyOperation and @AuditLogTarget: recorded "
        + "(@AuditLogTarget takes precedence)")
    void auditLogTargetTakesPrecedence() throws Exception {
      mockMvc.perform(post("/account/sample/showPersonalData")).andExpect(status().isOk());

      assertThat(onlyRecord().details().get("handler"))
          .isEqualTo("SampleController#showPersonalData");
    }

    @Test
    @DisplayName("GET with @ReadOnlyOperation(false): recorded")
    void getWithReadOnlyOperationFalse() throws Exception {
      mockMvc.perform(get("/account/sample/getThatWrites")).andExpect(status().isOk());

      assertThat(onlyRecord().details().get("handler"))
          .isEqualTo("SampleController#getThatWrites");
    }

    @Test
    @DisplayName("POST to a controller with @AuditLogTarget(false): not recorded")
    void classAnnotation() throws Exception {
      mockMvc.perform(post("/account/notRecorded/action")).andExpect(status().isOk());

      assertThat(writer.records).isEmpty();
    }

    @Test
    @DisplayName("asynchronous processing: recorded once, when it completes "
        + "(what was added before it is not taken over)")
    void async() throws Exception {
      MvcResult result = mockMvc.perform(post("/account/sample/async"))
          .andExpect(request().asyncStarted()).andReturn();
      assertThat(writer.records).isEmpty();

      mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());

      assertThat(onlyRecord().targets()).isEmpty();
      assertThat(onlyRecord().result()).isEqualTo(AuditLogResult.SUCCESS);
    }
  }

  @Nested
  @DisplayName("recorded items")
  class Items {

    @Test
    @DisplayName("request items and a detail added while processing (in that order), targets and "
        + "attributes")
    void items() throws Exception {
      login(user("user1"));

      mockMvc.perform(post("/account/sample/ok").with(r -> {
        r.setRemoteAddr("192.0.2.1");
        return r;
      }).session(new MockHttpSession(null, "ABCDEFGHIJKLMNOP"))).andExpect(status().isOk());

      Entry record = onlyRecord();
      assertThat(record.eventType()).isEqualTo(AuditLogEventType.OPERATION);
      assertThat(record.result()).isEqualTo(AuditLogResult.SUCCESS);
      assertThat(record.actor()).isEqualTo("user1");
      assertThat(record.originalActor()).isNull();
      assertThat(record.details()).containsExactly(entry("clientIp", "192.0.2.1"),
          entry("sessionIdSuffix", "IJKLMNOP"), entry("httpMethod", "POST"),
          entry("uri", "/account/sample/ok"), entry("handler", "SampleController#ok"),
          entry("loginState", "account"));
      assertThat(record.targets()).containsExactly(new TargetData("Sample", "1"));
      assertThat(record.error()).isNull();
      assertThat(record.attributes()).containsExactlyEntriesOf(Map.of("systemCode", "S1"));
      AuditLogContext seen = Objects.requireNonNull(SampleController.seen);
      assertThat(record.operationId()).isEqualTo(seen.getOperationId());
    }

    @Test
    @DisplayName("while processing: who does the operation is kept for the data change history, "
        + "even in a request not recorded")
    void keptWhileProcessing() throws Exception {
      Authentication admin = user("admin1");
      login(UsernamePasswordAuthenticationToken.authenticated("user1", null,
          List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT"),
              new SwitchUserGrantedAuthority("ROLE_PREVIOUS_ADMINISTRATOR", admin))));

      mockMvc.perform(get("/account/sample/get")).andExpect(status().isOk());

      AuditLogContext seen = Objects.requireNonNull(SampleController.seen);
      assertThat(seen.getActor()).isEqualTo("user1");
      assertThat(seen.getOriginalActor()).isEqualTo("admin1");
      assertThat(seen.getOperationId()).isNotEmpty();
      assertThat(writer.records).isEmpty();
    }

    @Test
    @DisplayName("not logged in: actor is null, and no session is created")
    void notLoggedIn() throws Exception {
      MvcResult result =
          mockMvc.perform(post("/account/sample/ok")).andExpect(status().isOk()).andReturn();

      assertThat(onlyRecord().actor()).isNull();
      assertThat(onlyRecord().details().get("sessionIdSuffix")).isNull();
      assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("during a switch user: the switched user and the administrator")
    void switchUser() throws Exception {
      Authentication admin = user("admin1");
      login(UsernamePasswordAuthenticationToken.authenticated("user1", null,
          List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT"),
              new SwitchUserGrantedAuthority("ROLE_PREVIOUS_ADMINISTRATOR", admin))));

      mockMvc.perform(post("/account/sample/ok")).andExpect(status().isOk());

      assertThat(onlyRecord().actor()).isEqualTo("user1");
      assertThat(onlyRecord().originalActor()).isEqualTo("admin1");
    }

    @Test
    @DisplayName("not recorded requests leave nothing behind for the next one on the thread")
    void noLeftover() throws Exception {
      mockMvc.perform(get("/account/sample/get"));
      mockMvc.perform(post("/account/sample/violation"));
      mockMvc.perform(post("/account/sample/back"));

      AuditLogUtil.addTarget("Leftover", 1L);
      assertThat(AuditLogContext.end()).isNull();
    }
  }

  @Nested
  @DisplayName("result")
  class Result {

    @Test
    @DisplayName("ViolationException handled by a redirect: business error with message IDs")
    void violation() throws Exception {
      mockMvc.perform(post("/account/sample/violation")).andExpect(status().is3xxRedirection());

      assertThat(onlyRecord().result()).isEqualTo(AuditLogResult.BUSINESS_ERROR);
      assertThat(onlyRecord().error())
          .isEqualTo(ViolationException.class.getName() + " [MSG_ID_1, MSG_ID_2]");
    }

    @Test
    @DisplayName("exception handled with 500: system error")
    void handledError() throws Exception {
      mockMvc.perform(post("/account/sample/handledError"))
          .andExpect(status().isInternalServerError());

      assertThat(onlyRecord().result()).isEqualTo(AuditLogResult.SYSTEM_ERROR);
      assertThat(onlyRecord().error()).isEqualTo(IllegalStateException.class.getName());
    }

    @Test
    @DisplayName("exception no handler handles: system error")
    void unhandledError() {
      assertThatThrownBy(() -> mockMvc.perform(post("/account/sample/unhandledError")))
          .hasCauseInstanceOf(UnhandledException.class);

      assertThat(onlyRecord().result()).isEqualTo(AuditLogResult.SYSTEM_ERROR);
      assertThat(onlyRecord().error()).isEqualTo(UnhandledException.class.getName());
    }

    @Test
    @DisplayName("4xx without an exception: business error")
    void badRequest() throws Exception {
      mockMvc.perform(post("/account/sample/badRequest")).andExpect(status().isBadRequest());

      assertThat(onlyRecord().result()).isEqualTo(AuditLogResult.BUSINESS_ERROR);
      assertThat(onlyRecord().error()).isNull();
    }
  }

  @Nested
  @DisplayName("resultOf")
  class ResultOf {

    @SuppressWarnings("null")
    static Stream<Arguments> cases() {
      Throwable violation = new ViolationException(new Violations().add("MSG_ID"));
      Throwable other = new IllegalStateException();
      return Stream.of(
          Arguments.of("unhandled exception", other, null, 200, AuditLogResult.SYSTEM_ERROR),
          Arguments.of("handled violation, 302", null, violation, 302,
              AuditLogResult.BUSINESS_ERROR),
          Arguments.of("handled violation, 500", null, violation, 500,
              AuditLogResult.BUSINESS_ERROR),
          Arguments.of("handled other, 302", null, other, 302,
              AuditLogResult.BUSINESS_ERROR),
          Arguments.of("handled other, 500", null, other, 500, AuditLogResult.SYSTEM_ERROR),
          Arguments.of("no exception, 200", null, null, 200, AuditLogResult.SUCCESS),
          Arguments.of("no exception, 302", null, null, 302, AuditLogResult.SUCCESS),
          Arguments.of("no exception, 404", null, null, 404,
              AuditLogResult.BUSINESS_ERROR),
          Arguments.of("no exception, 503", null, null, 503, AuditLogResult.SYSTEM_ERROR));
    }

    @ParameterizedTest(name = "{0}: {4}")
    @MethodSource("cases")
    @DisplayName("decides the result from the exceptions and the status")
    void resultOf(String description, Throwable unhandled, Throwable handled, int status,
        AuditLogResult expected) {
      assertThat(AuditLogInterceptor.resultOf(unhandled, handled, status))
          .isEqualTo(expected);
    }
  }
}
