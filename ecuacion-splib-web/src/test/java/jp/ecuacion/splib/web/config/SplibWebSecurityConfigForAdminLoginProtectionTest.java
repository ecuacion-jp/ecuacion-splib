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
package jp.ecuacion.splib.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import jp.ecuacion.splib.web.adminlogin.AdminLoginTestSupport.CapturingMailUtil;
import jp.ecuacion.splib.web.adminlogin.SplibAdminAccountProvider;
import jp.ecuacion.splib.web.adminlogin.SplibAdminAccountState;
import jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection;
import jp.ecuacion.splib.web.adminlogin.SplibAdminLoginProtection.VerifyResult;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Tests the admin filter chain with the protection of the admin login
 *     ({@code SplibAdminLoginProtection}): e2e A-20 and A-24 of {@code inquiry}.
 *
 * <p>The details of the login code, the session check and the attempt limit are tested in
 *     {@code SplibAdminLoginProtectionTest} and {@code AdminSessionCheckFilterTest}.</p>
 */
@DisplayName("SplibWebSecurityConfigForAdmin: protection of the admin login")
class SplibWebSecurityConfigForAdminLoginProtectionTest {

  private static final String ADMIN = "admin@example.com";
  private static final String PASSWORD = "password";
  private static final String HOME = "/admin/home/page";

  @Configuration
  @EnableWebSecurity
  static class AppConfig extends SplibWebSecurityConfigForAdmin {

    /** Needed by the matcher of static resources, provided by the auto-configuration in apps. */
    @Bean
    DispatcherServletPath dispatcherServletPath() {
      return () -> "/";
    }

    @Bean
    UserDetailsService userDetailsService() {
      return new InMemoryUserDetailsManager(User.withUsername(ADMIN).password("{noop}" + PASSWORD)
          .roles(ADMIN_FULL_ACCESS).build());
    }

    @Bean
    SplibAdminAccountProvider accountProvider() {
      return loginId -> ADMIN.equals(loginId) ? new SplibAdminAccountState(ADMIN, "stamp")
          : null;
    }

    @Bean
    CapturingMailUtil mailUtil() {
      return new CapturingMailUtil();
    }

    @Override
    protected String getDefaultSuccessUrl() {
      return HOME;
    }

    @Override
    protected String getLoginNeededPage() {
      return "/public/adminLogin/page";
    }

    @Override
    protected List<AuthorizationBean> getRoleInfo() {
      return new ArrayList<>();
    }

    @Override
    protected List<AuthorizationBean> getAuthorityInfo() {
      return new ArrayList<>();
    }

    @Override
    protected String getAccessDeniedPage() {
      return "/public/adminLogin/page?accessDenied";
    }
  }

  /** The result of a request. */
  record Result(boolean reached, @Nullable String redirectedUrl) {}

  private @Nullable AnnotationConfigWebApplicationContext context;
  private final MockHttpSession session = new MockHttpSession();

  @AfterEach
  void tearDown() {
    AnnotationConfigWebApplicationContext ctx = context;
    if (ctx != null) {
      ctx.close();
    }

    SecurityContextHolder.clearContext();
  }

  private AnnotationConfigWebApplicationContext start(boolean enabled) {
    AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
    MockServletContext servletContext = new MockServletContext();
    ctx.setServletContext(servletContext);
    if (enabled) {
      Map<String, Object> props = new HashMap<>();
      props.put(SplibAdminLoginProtection.PROPERTY_LOGIN_CODE_ENABLED, "true");
      props.put(SplibAdminLoginProtection.PROPERTY_SESSION_CHECK_ENABLED, "true");
      props.put(SplibAdminLoginProtection.PROPERTY_ATTEMPT_LIMIT_ENABLED, "true");
      ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", props));
    }

    ctx.register(AppConfig.class);
    ctx.refresh();
    servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
        ctx);
    context = ctx;
    return ctx;
  }

  private AnnotationConfigWebApplicationContext ctx() {
    return Objects.requireNonNull(context);
  }

  private Result perform(String method, String uri, Map<String, String> params,
      @Nullable AtomicReference<@Nullable String> csrfHolder) throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest(ctx().getServletContext(), method, uri);
    request.setServletPath(uri);
    request.setSession(session);
    params.forEach(request::addParameter);
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean[] reached = {false};
    FilterChain chain = (req, res) -> {
      reached[0] = true;
      if (csrfHolder != null
          && req.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken token) {
        csrfHolder.set(token.getToken());
      }
    };
    ctx().getBean("springSecurityFilterChain", FilterChainProxy.class)
        .doFilter(request, response, chain);
    return new Result(reached[0], response.getRedirectedUrl());
  }

  private Result get(String uri) throws Exception {
    return perform("GET", uri, Map.of(), null);
  }

  private Result post(String uri, Map<String, String> params) throws Exception {
    AtomicReference<@Nullable String> csrf = new AtomicReference<>();
    perform("GET", "/public/adminLogin/page", Map.of(), csrf);
    Map<String, String> withCsrf = new HashMap<>(params);
    withCsrf.put("_csrf", Objects.requireNonNull(csrf.get()));
    return perform("POST", uri, withCsrf, null);
  }

  private Result login(String password) throws Exception {
    return post(SplibAdminLoginProtection.LOGIN_PROCESSING_PATH,
        Map.of(SplibAdminLoginProtection.USERNAME_PARAMETER, ADMIN, "adminLogin.password",
            password));
  }

  /** Enters the code sent by mail, as the page to enter the login code does. */
  private VerifyResult enterCode() {
    SecurityContextHolder.setContext((SecurityContext) Objects.requireNonNull(
        session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    VerifyResult result = ctx().getBean(SplibAdminLoginProtection.class)
        .verifyLoginCode(request, new MockHttpServletResponse(),
            ctx().getBean(CapturingMailUtil.class).lastCode());
    SecurityContextHolder.clearContext();
    return result;
  }

  @Nested
  @DisplayName("enabled")
  class Enabled {

    @Test
    @DisplayName("A-24: before the code, /admin/** and switching users move to the code page")
    void beforeCode() throws Exception {
      start(true);
      assertThat(login(PASSWORD).redirectedUrl())
          .isEqualTo(SplibAdminLoginProtection.LOGIN_CODE_PAGE_PATH);

      Result home = get(HOME);
      assertThat(home.reached()).isFalse();
      assertThat(home.redirectedUrl()).isEqualTo(SplibAdminLoginProtection.LOGIN_CODE_PAGE_PATH);
      assertThat(post("/admin/switchUser", Map.of("switchUser.username", "123")).reached())
          .isFalse();
      // the page to enter the code can be opened
      assertThat(get(SplibAdminLoginProtection.LOGIN_CODE_PAGE_PATH).reached()).isTrue();
    }

    @Test
    @DisplayName("A-24: after the right code, /admin/** can be used")
    void afterCode() throws Exception {
      start(true);
      login(PASSWORD);

      assertThat(enterCode()).isEqualTo(VerifyResult.OK);
      assertThat(get(HOME).reached()).isTrue();
      assertThat(post("/admin/switchUser", Map.of("switchUser.username", "123")).reached())
          .isTrue();
    }

    @Test
    @DisplayName("A-20: after 10 wrong passwords, the right one fails too, until it's reset")
    void attemptLimit() throws Exception {
      start(true);
      for (int i = 0; i < 10; i++) {
        assertThat(login("wrong").redirectedUrl()).isEqualTo("/public/adminLogin/page?error");
      }

      assertThat(login(PASSWORD).redirectedUrl()).isEqualTo("/public/adminLogin/page?error");
      assertThat(session.getAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNull();

      ctx().getBean(SplibAdminLoginProtection.class).resetPasswordFailures(ADMIN);
      assertThat(login(PASSWORD).redirectedUrl())
          .isEqualTo(SplibAdminLoginProtection.LOGIN_CODE_PAGE_PATH);
    }
  }

  @Nested
  @DisplayName("not enabled")
  class NotEnabled {

    @Test
    @DisplayName("the login moves to the default success url and /admin/** can be used")
    void unchanged() throws Exception {
      start(false);
      assertThat(login(PASSWORD).redirectedUrl()).isEqualTo(HOME);
      assertThat(get(HOME).reached()).isTrue();
    }

    @Test
    @DisplayName("wrong passwords are not limited")
    void noLimit() throws Exception {
      start(false);
      for (int i = 0; i < 11; i++) {
        login("wrong");
      }

      assertThat(login(PASSWORD).redirectedUrl()).isEqualTo(HOME);
    }
  }
}
