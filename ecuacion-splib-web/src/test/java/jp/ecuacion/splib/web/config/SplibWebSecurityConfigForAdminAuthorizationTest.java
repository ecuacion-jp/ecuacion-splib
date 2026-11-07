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
import java.util.List;
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
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
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Tests the roles allowed by the URLs, which limit the features an admin with a limited role can
 *     use. An URL not specified in {@code getRoleInfo()} needs {@code ADMIN_FULL_ACCESS}.
 */
@DisplayName("SplibWebSecurityConfigForAdmin: authorization of /admin/**")
class SplibWebSecurityConfigForAdminAuthorizationTest {

  private static final String LIMITED = "ADMIN_LIMITED_DATA_ACCESS";

  @Configuration
  @EnableWebSecurity
  static class AppConfig extends SplibWebSecurityConfigForAdmin {

    /** Needed by the matcher of static resources, provided by the auto-configuration in apps. */
    @Bean
    DispatcherServletPath dispatcherServletPath() {
      return () -> "/";
    }

    @Override
    protected String getDefaultSuccessUrl() {
      return "/admin/home/page";
    }

    @Override
    protected String getLoginNeededPage() {
      return "/public/adminLogin/page";
    }

    @Override
    protected List<AuthorizationBean> getRoleInfo() {
      List<AuthorizationBean> list = new ArrayList<>();
      list.add(new AuthorizationBean("/admin/inquiry/**", LIMITED));
      return list;
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

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private boolean reachesApp(String role, String uri) throws Exception {
    try (AnnotationConfigWebApplicationContext context =
        new AnnotationConfigWebApplicationContext()) {
      MockServletContext servletContext = new MockServletContext();
      context.setServletContext(servletContext);
      context.register(AppConfig.class);
      context.refresh();
      servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
          context);

      MockHttpSession session = new MockHttpSession();
      session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("user", null,
              List.of(new SimpleGrantedAuthority("ROLE_" + role)))));
      MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", uri);
      request.setServletPath(uri);
      request.setSession(session);

      boolean[] reached = {false};
      FilterChain chain = (req, res) -> reached[0] = true;
      context.getBean("springSecurityFilterChain", FilterChainProxy.class)
          .doFilter(request, new MockHttpServletResponse(), chain);
      return reached[0];
    }
  }

  @ParameterizedTest
  @CsvSource({"ADMIN_LIMITED_DATA_ACCESS, /admin/inquiry/searchList/page",
      "ADMIN_FULL_ACCESS, /admin/inquiry/searchList/page",
      "ADMIN_FULL_ACCESS, /admin/targetSystem/searchList/page"})
  @DisplayName("allows the specified roles and ADMIN_FULL_ACCESS")
  void allowed(String role, String uri) throws Exception {
    assertThat(reachesApp(role, uri)).isTrue();
  }

  @Test
  @DisplayName("denies a limited role on an URL which is not specified")
  void denied() throws Exception {
    assertThat(reachesApp(LIMITED, "/admin/targetSystem/searchList/page")).isFalse();
  }
}
