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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Logs the reason of an OAuth2 login failure and redirects to the login page.
 *
 * <p>The redirect has {@code ?oauth2Error}, so the login page doesn't show
 *     the message for id/password login, which is shown with {@code ?error}.
 *     The cause is not shown to the user. It's found in the log.
 *     When the user cancelled the login at the provider,
 *     the redirect has no parameters since it needs no explanation.</p>
 *
 * <p>The log level is {@code INFO} for the errors that happen in normal use,
 *     and {@code WARN} for the others, which may need a fix on the server.
 *     {@code authorization_request_not_found} is one of the former, since it's also caused by
 *     an expired session or by anyone accessing the callback url without logging in.
 *     {@code email_not_verified} (see {@link SplibOauth2OidcUserService}) is also one of
 *     the former, since it depends on the user's account at the provider.</p>
 *
 * <p>The error code and the description may come from the parameters of the callback,
 *     so the values are shortened. Their control characters are removed by the logger.
 *     Secret values like tokens are not passed to the log by this class,
 *     but the description and the cause's message are written by the libraries.</p>
 */
public class SplibOauth2AuthFailureHandler implements AuthenticationFailureHandler {

  private static final String AUTHORIZATION_REQUEST_NOT_FOUND = "authorization_request_not_found";

  /** Apple returns this instead of {@code access_denied}. */
  private static final String APPLE_USER_CANCELLED = "user_cancelled_authorize";

  private static final Set<String> CANCEL_ERROR_CODES =
      Set.of(OAuth2ErrorCodes.ACCESS_DENIED, APPLE_USER_CANCELLED);

  private static final int MAX_LOG_VALUE_LENGTH = 500;

  private final DetailLogger detailLog = new DetailLogger(this);

  private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

  private final String loginPage;

  /**
   * Constructs a new instance with {@code loginPage}.
   *
   * @param loginPage the path of the login page, without the context path
   */
  public SplibOauth2AuthFailureHandler(String loginPage) {
    this.loginPage = loginPage;
  }

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {
    OAuth2Error error =
        exception instanceof OAuth2AuthenticationException ex ? ex.getError() : null;
    String errorCode = error == null ? null : error.getErrorCode();
    String description = error == null ? exception.getMessage() : error.getDescription();
    boolean isCancelled = errorCode != null && CANCEL_ERROR_CODES.contains(errorCode);

    String message = "OAuth2 login failed. registrationId: "
        + shorten(StringUtils.substringAfterLast(request.getRequestURI(), "/")) + ", errorCode: "
        + shorten(errorCode) + ", description: " + shorten(description) + causeOf(exception);
    if (isCancelled || AUTHORIZATION_REQUEST_NOT_FOUND.equals(errorCode)
        || SplibOauth2OidcUserService.EMAIL_NOT_VERIFIED_ERROR_CODE.equals(errorCode)) {
      detailLog.info(message);
    } else {
      detailLog.warn(message);
    }

    redirectStrategy.sendRedirect(request, response,
        isCancelled ? loginPage : loginPage + "?oauth2Error");
  }

  private String causeOf(AuthenticationException exception) {
    Throwable cause = NestedExceptionUtils.getRootCause(exception);
    return cause == null ? ""
        : ", cause: " + cause.getClass().getName() + ": " + shorten(cause.getMessage());
  }

  private @Nullable String shorten(@Nullable String value) {
    return value == null ? null : StringUtils.abbreviate(value, MAX_LOG_VALUE_LENGTH);
  }
}
