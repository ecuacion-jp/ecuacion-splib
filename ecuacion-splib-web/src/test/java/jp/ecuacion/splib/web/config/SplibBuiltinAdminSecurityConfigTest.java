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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Tests that the built-in admin pages need the built-in admin's HTTP Basic credentials, and
 *     that it is independent of the app's own login stored in the session.
 */
@DisplayName("SplibBuiltinAdminSecurityConfig: HTTP Basic for /ecuacion-splib/admin/**")
class SplibBuiltinAdminSecurityConfigTest {

  private static final String URI = "/ecuacion-splib/admin/config/page";
  private static final String PREFIX = "jp.ecuacion.splib.web.builtin-admin-login";
  private static final String PASSWORD = "builtin-pw";
  private static final String CONTEXT_KEY =
      HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

  private final Map<String, String> properties = new HashMap<>();

  /** The app's own login, whose credentials must not authenticate the built-in admin pages. */
  @Configuration
  @EnableWebSecurity
  static class AppConfig {

    @Bean
    UserDetailsService userDetailsService() {
      return new InMemoryUserDetailsManager(User.withUsername("ecuacion-splib")
          .password("{noop}app-pw").roles("BUILTIN_ADMIN").build());
    }
  }

  private static class Result {
    private boolean reached;
    private int status;
  }

  @BeforeEach
  void setUp() {
    properties.put(PREFIX + ".password-plain", PASSWORD);
    PropertiesFileUtil.setApplicationResolver(properties::get);
    PropertiesFileUtil.clearCache();
  }

  @AfterEach
  void tearDown() {
    PropertiesFileUtil.setApplicationResolver(null);
    PropertiesFileUtil.clearCache();
    SecurityContextHolder.clearContext();
  }

  private Result access(@Nullable String username, @Nullable String password,
      MockHttpSession session) throws Exception {
    try (AnnotationConfigWebApplicationContext context =
        new AnnotationConfigWebApplicationContext()) {
      MockServletContext servletContext = new MockServletContext();
      context.setServletContext(servletContext);
      context.register(AppConfig.class, SplibBuiltinAdminSecurityConfig.class);
      context.refresh();
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
          context);

      MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", URI);
      request.setServletPath(URI);
      request.setSession(session);
      if (username != null) {
        request.addHeader("Authorization", "Basic " + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));
      }

      Result result = new Result();
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain chain = (req, res) -> result.reached = true;
      context.getBean("springSecurityFilterChain", FilterChainProxy.class).doFilter(request,
          response, chain);
      result.status = response.getStatus();
      return result;
    }
  }

  private MockHttpSession sessionLoggedInToApp(String role) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(CONTEXT_KEY,
        new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("user", null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role)))));
    return session;
  }

  @Test
  @DisplayName("allows the built-in admin's credentials")
  void allowed() throws Exception {
    Result result = access("ecuacion-splib", PASSWORD, new MockHttpSession());
    assertThat(result.reached).isTrue();
  }

  @Test
  @DisplayName("asks an anonymous user for the credentials")
  void anonymous() throws Exception {
    Result result = access(null, null, new MockHttpSession());
    assertThat(result.reached).isFalse();
    assertThat(result.status).isEqualTo(401);
  }

  @Test
  @DisplayName("denies a wrong password")
  void wrongPassword() throws Exception {
    Result result = access("ecuacion-splib", "wrong", new MockHttpSession());
    assertThat(result.reached).isFalse();
    assertThat(result.status).isEqualTo(401);
  }

  @Test
  @DisplayName("denies the app user's credentials (no fallback to the app's UserDetailsService)")
  void appUserCredentials() throws Exception {
    Result result = access("ecuacion-splib", "app-pw", new MockHttpSession());
    assertThat(result.reached).isFalse();
    assertThat(result.status).isEqualTo(401);
  }

  @Test
  @DisplayName("ignores the app's login in the session, even with ROLE_BUILTIN_ADMIN")
  void appLoginInSession() throws Exception {
    Result result = access(null, null, sessionLoggedInToApp("BUILTIN_ADMIN"));
    assertThat(result.reached).isFalse();
    assertThat(result.status).isEqualTo(401);
  }

  @Test
  @DisplayName("keeps the app's login in the session as it is")
  void keepsAppLogin() throws Exception {
    MockHttpSession session = sessionLoggedInToApp("ADMIN_FULL_ACCESS");
    Object appContext = session.getAttribute(CONTEXT_KEY);

    Result result = access("ecuacion-splib", PASSWORD, session);

    assertThat(result.reached).isTrue();
    assertThat(session.getAttribute(CONTEXT_KEY))
        .isSameAs(appContext);
  }

  @Test
  @DisplayName("does not store the built-in admin in the session")
  void notStoredInSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    access("ecuacion-splib", PASSWORD, session);
    assertThat(session.getAttribute(CONTEXT_KEY))
        .isNull();
  }

  @Test
  @DisplayName("denies every request when no password is configured")
  void notConfigured() throws Exception {
    properties.clear();
    Result result = access("ecuacion-splib", PASSWORD, new MockHttpSession());
    assertThat(result.reached).isFalse();
    assertThat(result.status).isEqualTo(401);
  }

  @Test
  @DisplayName("answers 500 when both password properties are configured")
  void misconfigured() throws Exception {
    properties.put(PREFIX + ".password-bcrypt", "$2a$10$dummy");
    Result result = access("ecuacion-splib", PASSWORD, new MockHttpSession());
    assertThat(result.reached).isFalse();
    assertThat(result.status).isEqualTo(500);
  }
}
