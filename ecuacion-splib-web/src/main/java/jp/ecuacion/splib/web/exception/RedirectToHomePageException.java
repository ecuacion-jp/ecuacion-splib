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
package jp.ecuacion.splib.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.web.constant.LoginState;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Transitions to home page.
 *
 * <p>When the current request is under a logged-in area ({@code /account/**} or
 *     {@code /admin/**}) and {@code jp.ecuacion.splib.web.navbar.default-page-path-account} /
 *     {@code -admin} is set, transitions to that page instead of
 *     {@code jp.ecuacion.splib.web.home-page}, so that a logged-in user who hits an error isn't
 *     sent to the page for the other kind of user (e.g. an administrator sent to the end-user
 *     login page). Falls back to {@code jp.ecuacion.splib.web.home-page} when the current
 *     request is that page itself, to avoid a redirect loop.</p>
 */
public class RedirectToHomePageException extends RedirectException {

  private static final long serialVersionUID = 1L;
  private static final String HOME_PAGE_PATH =
      PropertiesFileUtil.getApplication("jp.ecuacion.splib.web.home-page");

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException() {
    this((@Nullable Level) null, (@Nullable String) null, (@Nullable String) null);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(String messageId, String... messageArgs) {
    this((@Nullable Level) null, (@Nullable String) null, messageId, messageArgs);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(@Nullable Level logLevel, @Nullable String logString) {
    this(logLevel, logString, (@Nullable String) null);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(@Nullable Level logLevel, @Nullable String logString,
      @Nullable String messageId, String... messageArgs) {
    super(properHomePagePath(), logLevel, logString, messageId, messageArgs);
  }

  /**
   * See {@link RedirectException}.
   */
  public RedirectToHomePageException(@Nullable Level logLevel, String messageId,
      String[] messageArgs) {
    super(properHomePagePath(), logLevel, messageId, messageArgs);
  }

  private static String properHomePagePath() {
    String loginStateDependentPath = loginStateDependentDefaultPagePath();
    return loginStateDependentPath != null ? loginStateDependentPath
        : withLeadingSlash(HOME_PAGE_PATH);
  }

  private static @Nullable String loginStateDependentDefaultPagePath() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
      return null;
    }

    HttpServletRequest request = attrs.getRequest();
    String loginState;
    try {
      loginState = new SplibLoginStateUtil(request, request.getServletContext()).getLoginState();

    } catch (RuntimeException ex) {
      // The URL doesn't follow the "/<loginState>/..." convention (e.g. a URL that doesn't exist).
      return null;
    }

    if (!loginState.equals(LoginState.ACCOUNT.getCode())
        && !loginState.equals(LoginState.ADMIN.getCode())) {
      return null;
    }

    String path = PropertiesFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.navbar.default-page-path-" + loginState, null);
    if (path == null || path.isEmpty()) {
      return null;
    }

    path = withLeadingSlash(path);
    String currentPath = request.getRequestURI().substring(request.getContextPath().length());
    return currentPath.equals(path) ? null : path;
  }

  private static String withLeadingSlash(String path) {
    return (path.startsWith("/") ? "" : "/") + path;
  }
}
