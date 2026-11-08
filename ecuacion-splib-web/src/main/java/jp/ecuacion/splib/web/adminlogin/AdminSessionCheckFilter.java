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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import jp.ecuacion.splib.webmvc.util.SwitchUserUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Checks the session of the admin on every request to {@code /admin/**}, and while switched to
 *     another user (about the admin who started it).
 *
 * <p>When the session cannot be used anymore
 *     ({@link SplibAdminLoginProtection#isSessionValid}), it logs out the session and redirects
 *     to the admin login page.</p>
 *
 * <p>It runs after the Spring Security filters, so the requests to {@code /admin/**} reaching it
 *     have passed the authorization of the admin filter chain (an admin who logged in on the
 *     admin login page and, with the login code, entered the code). It runs before
 *     {@code SwitchUserFilter}, so an admin who cannot use the session cannot switch users.</p>
 */
class AdminSessionCheckFilter extends OncePerRequestFilter {

  private static final RequestMatcher ADMIN_MATCHER =
      PathPatternRequestMatcher.withDefaults().matcher("/admin/**");

  private final SplibAdminLoginProtection protection;

  AdminSessionCheckFilter(SplibAdminLoginProtection protection) {
    this.protection = protection;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String loginId = getAdminLoginId(request, authentication);
    if (loginId != null && !protection.isSessionValid(request, loginId)) {
      protection.logoutAndRedirect(request, response, authentication,
          SplibAdminLoginProtection.ADMIN_LOGIN_PAGE_PATH);
      return;
    }

    filterChain.doFilter(request, response);
  }

  /** Returns the login ID of the admin to check, {@code null} if the request is not checked. */
  private static @Nullable String getAdminLoginId(HttpServletRequest request,
      @Nullable Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }

    Authentication source = SwitchUserUtil.getSwitchUserSource(authentication);
    if (source != null) {
      return source.getName();
    }

    return ADMIN_MATCHER.matches(request) ? authentication.getName() : null;
  }
}
