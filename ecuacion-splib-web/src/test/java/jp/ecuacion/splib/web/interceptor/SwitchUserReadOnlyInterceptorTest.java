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
package jp.ecuacion.splib.web.interceptor;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.splib.core.annotation.ReadOnlyOperation;
import jp.ecuacion.splib.web.config.SplibWebSecurityConfigForSwitchUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
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
 * Tests for {@link SwitchUserReadOnlyInterceptor}, through a Spring MVC context with a
 * {@link SplibWebSecurityConfigForSwitchUser} that enables it, so that it is registered the way
 * applications get it.
 */
@DisplayName("SwitchUserReadOnlyInterceptor")
@SpringJUnitWebConfig(SwitchUserReadOnlyInterceptorTest.TestConfig.class)
class SwitchUserReadOnlyInterceptorTest {

  @Controller
  @RequestMapping("/account/sample")
  static class SampleController {

    @GetMapping("page")
    @ResponseBody
    String page() {
      return "ok";
    }

    @PostMapping("save")
    @ResponseBody
    String save() {
      return "ok";
    }

    @ReadOnlyOperation
    @PostMapping("showDetail")
    @ResponseBody
    String showDetail() {
      return "ok";
    }
  }

  /** Inherits {@code showDetail()}'s annotation through the overridden method. */
  @Controller
  @RequestMapping("/account/sub")
  static class SubController extends SampleController {

    @Override
    @PostMapping("showDetail")
    @ResponseBody
    String showDetail() {
      return "ok";
    }
  }

  /** Answers the message ID, instead of redirecting as {@code SplibWebExceptionHandler} does. */
  @ControllerAdvice
  static class Advice {

    @ExceptionHandler(ViolationException.class)
    ResponseEntity<String> violation(ViolationException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(ex.getViolations().getBusinessViolations().get(0).getMessageId());
    }
  }

  @Configuration
  static class ReadOnlyConfig extends SplibWebSecurityConfigForSwitchUser {

    ReadOnlyConfig() {
      super(mock(UserDetailsService.class));
    }

    @Override
    protected boolean isReadOnlyWhileSwitched() {
      return true;
    }

    @Override
    protected String getSwitchingUserDonePagePath() {
      return "/account/home/page";
    }

    @Override
    protected String getSwitchingUserFailurePagePath() {
      return "/admin/home/page";
    }

    @Override
    protected String getExitingUserDonePagePath() {
      return "/admin/home/page";
    }
  }

  @Configuration
  @EnableWebMvc
  @Import({ReadOnlyConfig.class, SampleController.class, SubController.class, Advice.class})
  static class TestConfig {
  }

  @Autowired
  private WebApplicationContext context;

  @SuppressWarnings("null")
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static void loginAsSwitchedUser() {
    Authentication admin = UsernamePasswordAuthenticationToken.authenticated("admin1", null,
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    SecurityContextHolder.getContext()
        .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("user1", null,
            List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT"),
                new SwitchUserGrantedAuthority("ROLE_PREVIOUS_ADMINISTRATOR", admin))));
  }

  private static void loginAsUser() {
    SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken
        .authenticated("user1", null, List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT"))));
  }

  @Test
  @DisplayName("switched, POST: rejected with the message")
  void switchedPostIsRejected() throws Exception {
    loginAsSwitchedUser();

    mockMvc.perform(post("/account/sample/save")).andExpect(status().isConflict())
        .andExpect(content().string(SwitchUserReadOnlyInterceptor.MSG_ID));
  }

  @Test
  @DisplayName("switched, GET: allowed")
  void switchedGetIsAllowed() throws Exception {
    loginAsSwitchedUser();

    mockMvc.perform(get("/account/sample/page")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("switched, POST with @ReadOnlyOperation: allowed")
  void switchedReadOnlyPostIsAllowed() throws Exception {
    loginAsSwitchedUser();

    mockMvc.perform(post("/account/sample/showDetail")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("switched, POST to an override of a @ReadOnlyOperation method: allowed")
  void switchedInheritedReadOnlyPostIsAllowed() throws Exception {
    loginAsSwitchedUser();

    mockMvc.perform(post("/account/sub/showDetail")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("not switched, POST: allowed")
  void notSwitchedPostIsAllowed() throws Exception {
    loginAsUser();

    mockMvc.perform(post("/account/sample/save")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("not logged in, POST: allowed")
  void notLoggedInPostIsAllowed() throws Exception {
    mockMvc.perform(post("/account/sample/save")).andExpect(status().isOk());
  }
}
