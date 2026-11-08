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

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.core.util.SplibAttemptLimiter;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import jp.ecuacion.splib.webmvc.util.SwitchUserUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.DefaultAuthorizationManagerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.DelegatingMissingAuthorityAccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Provides the protection of the admin login ({@code SplibWebSecurityConfigForAdmin}).
 *
 * <p>It's a bean registered by {@code SplibWebSecurityConfigForAdmin}. Each of the following is
 *     enabled by a property, and none of them changes anything unless enabled.</p>
 *
 * <ul>
 * <li><strong>Login code</strong> ({@code jp.ecuacion.splib.web.admin-login.login-code.enabled}):
 *     after the password, a 6-digit code is sent by mail, and {@code /admin/**} cannot be used
 *     until it's entered on {@code /public/adminLoginCode/page}. It uses the multi-factor
 *     authentication of Spring Security: the pages require {@code FACTOR_OTT}, which is added to
 *     the {@code Authentication} when the code is entered. The code is valid for
 *     {@link #LOGIN_CODE_VALID_DURATION}, and cannot be used after
 *     {@link #LOGIN_CODE_MAX_FAILURES} wrong codes. The wrong codes are also counted per login ID
 *     across the codes ({@code code-failure}), since logging in again issues a new code.
 *     Resending the code is limited ({@code code-resend}). Failing to send the mail does not stop
 *     the login: it's logged, and the code can be resent.</li>
 * <li><strong>Session check</strong>
 *     ({@code jp.ecuacion.splib.web.admin-login.session-check.enabled}): on every request to
 *     {@code /admin/**}, and while switched to another user (about the admin who started it), the
 *     session is logged out when the account cannot log in anymore or its
 *     {@link SplibAdminAccountState#stampSource()} has changed since the login. Roles are kept in
 *     the session from the login, so without it, invalidating an account or changing its role or
 *     password does not affect the sessions already logged in.</li>
 * <li><strong>Attempt limit</strong>
 *     ({@code jp.ecuacion.splib.web.admin-login.attempt-limit.enabled}): a login ID with too many
 *     wrong passwords ({@code password-failure}) is rejected without checking the password. The
 *     page is the same as other login failures. The count is removed when the login succeeds, or
 *     by {@link #resetPasswordFailures(String)}.</li>
 * </ul>
 *
 * <p>The numbers of attempts and the windows are set by
 *     {@code jp.ecuacion.splib.web.admin-login.<name>.max-attempts} and
 *     {@code jp.ecuacion.splib.web.admin-login.<name>.window-minutes}, where {@code <name>} is
 *     {@code password-failure} (default: 10 times in 15 minutes), {@code code-resend} (5 times in
 *     60 minutes) or {@code code-failure} (10 times in 60 minutes). They are counted in memory by
 *     {@link SplibAttemptLimiter}, so they are lost when the app restarts, and counted per server
 *     when the app runs on more than one server.</p>
 *
 * <p>The login code and the session check need a {@link SplibAdminAccountProvider} bean. The
 *     login code also needs the mail settings of {@link SplibMailUtil}.</p>
 *
 * <p>Apps call {@link #recordSessionStamp(HttpServletRequest)} after an admin changes the own
 *     password (to keep the session), and {@link #resetPasswordFailures(String)} after a password
 *     is set by a link sent by mail (the owner is verified by mail).</p>
 */
public class SplibAdminLoginProtection {

  private static final String PROPERTY_PREFIX = "jp.ecuacion.splib.web.admin-login.";

  /** The property to enable the login code. */
  public static final String PROPERTY_LOGIN_CODE_ENABLED = PROPERTY_PREFIX + "login-code.enabled";

  /** The property to enable the session check. */
  public static final String PROPERTY_SESSION_CHECK_ENABLED =
      PROPERTY_PREFIX + "session-check.enabled";

  /** The property to enable the attempt limit of wrong passwords. */
  public static final String PROPERTY_ATTEMPT_LIMIT_ENABLED =
      PROPERTY_PREFIX + "attempt-limit.enabled";

  /** The path of the page to enter the login code. */
  public static final String LOGIN_CODE_PAGE_PATH = "/public/adminLoginCode/page";

  /** The path of the admin login page. */
  public static final String ADMIN_LOGIN_PAGE_PATH = "/public/adminLogin/page";

  /** The path the admin login page posts to. */
  public static final String LOGIN_PROCESSING_PATH = "/public/adminLogin/action";

  /** The parameter of the username on the admin login page. */
  public static final String USERNAME_PARAMETER = "adminLogin.username";

  /** The duration in which the login code is valid. */
  public static final Duration LOGIN_CODE_VALID_DURATION = Duration.ofMinutes(10);

  /** The number of wrong codes which makes the login code unusable. */
  public static final int LOGIN_CODE_MAX_FAILURES = 5;

  private static final String SESSION_KEY_PREFIX = "jp.ecuacion.splib.web.adminLogin.";
  private static final String SESSION_KEY_LOGIN_ID = SESSION_KEY_PREFIX + "loginId";
  private static final String SESSION_KEY_STAMP = SESSION_KEY_PREFIX + "stamp";
  private static final String SESSION_KEY_CODE = SESSION_KEY_PREFIX + "code";
  private static final String SESSION_KEY_CODE_EXPIRES_AT = SESSION_KEY_PREFIX + "codeExpiresAt";
  private static final String SESSION_KEY_CODE_FAILURES = SESSION_KEY_PREFIX + "codeFailures";

  private static final String MSG_MAIL_TITLE = "jp.ecuacion.splib.web.adminLoginCode.mail.title";
  private static final String MSG_MAIL_BODY = "jp.ecuacion.splib.web.adminLoginCode.mail.body";

  /** The state of the login code of the request. */
  public enum LoginCodeState {
    /** Not logged in by the admin login page. */
    NOT_LOGGED_IN,
    /** Logged in by the password, waiting for the login code. */
    PENDING,
    /** The login code has been entered. */
    VERIFIED
  }

  /** The result of {@link #verifyLoginCode}. */
  public enum VerifyResult {
    /** The code is right. The pages under {@code /admin/**} can be used. */
    OK,
    /** The code is wrong. */
    WRONG,
    /** The code has expired, has been entered wrong too many times, or has not been issued. */
    UNUSABLE,
    /** Wrong codes of the login ID have been entered too many times. */
    TOO_MANY_FAILURES,
    /** The account cannot log in anymore. The session should be logged out. */
    ACCOUNT_UNAVAILABLE,
    /** The request is not waiting for the login code. */
    NOT_PENDING
  }

  /** The result of {@link #resendLoginCode}. */
  public enum ResendResult {
    /** A new code has been sent. */
    SENT,
    /** The code has been resent too many times. */
    LIMIT_EXCEEDED,
    /** Failed to send the mail. */
    SEND_FAILED,
    /** The account cannot log in anymore. The session should be logged out. */
    ACCOUNT_UNAVAILABLE,
    /** The request is not waiting for the login code. */
    NOT_PENDING
  }

  private final DetailLogger detailLog = new DetailLogger(this);

  private final boolean loginCodeEnabled;
  private final boolean sessionCheckEnabled;
  private final boolean attemptLimitEnabled;
  private final @Nullable SplibAdminAccountProvider accountProvider;
  private final @Nullable SplibMailUtil mailUtil;
  private final String defaultSuccessUrl;
  private final SplibAttemptLimiter passwordFailureLimiter;
  private final SplibAttemptLimiter codeResendLimiter;
  private final SplibAttemptLimiter codeFailureLimiter;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  /**
   * Constructs a new instance with {@code environment}, {@code accountProvider},
   *     {@code mailUtil} and {@code defaultSuccessUrl}.
   *
   * @param environment the environment to read the properties from
   * @param accountProvider the provider of admin accounts, may be {@code null} when neither the
   *     login code nor the session check is enabled
   * @param mailUtil the mail util, may be {@code null} when the login code is not enabled
   * @param defaultSuccessUrl the url when the login procedure successfully ended
   * @throws IllegalStateException if a feature is enabled without what it needs
   */
  public SplibAdminLoginProtection(Environment environment,
      @Nullable SplibAdminAccountProvider accountProvider, @Nullable SplibMailUtil mailUtil,
      String defaultSuccessUrl) {
    this(environment, accountProvider, mailUtil, defaultSuccessUrl, Clock.systemUTC());
  }

  SplibAdminLoginProtection(Environment environment,
      @Nullable SplibAdminAccountProvider accountProvider, @Nullable SplibMailUtil mailUtil,
      String defaultSuccessUrl, Clock clock) {
    this.loginCodeEnabled =
        environment.getProperty(PROPERTY_LOGIN_CODE_ENABLED, Boolean.class, false);
    this.sessionCheckEnabled =
        environment.getProperty(PROPERTY_SESSION_CHECK_ENABLED, Boolean.class, false);
    this.attemptLimitEnabled =
        environment.getProperty(PROPERTY_ATTEMPT_LIMIT_ENABLED, Boolean.class, false);

    if ((loginCodeEnabled || sessionCheckEnabled) && accountProvider == null) {
      throw new IllegalStateException("A bean of SplibAdminAccountProvider is needed when "
          + PROPERTY_LOGIN_CODE_ENABLED + " or " + PROPERTY_SESSION_CHECK_ENABLED + " is true.");
    }

    if (loginCodeEnabled && mailUtil == null) {
      throw new IllegalStateException(
          "A bean of SplibMailUtil is needed when " + PROPERTY_LOGIN_CODE_ENABLED + " is true.");
    }

    this.accountProvider = accountProvider;
    this.mailUtil = mailUtil;
    this.defaultSuccessUrl = defaultSuccessUrl;
    this.clock = clock;
    this.passwordFailureLimiter = limiter(environment, "password-failure", 10, 15, clock);
    this.codeResendLimiter = limiter(environment, "code-resend", 5, 60, clock);
    this.codeFailureLimiter = limiter(environment, "code-failure", 10, 60, clock);
  }

  private static SplibAttemptLimiter limiter(Environment environment, String name,
      int defaultMaxAttempts, int defaultWindowMinutes, Clock clock) {
    int maxAttempts = environment.getProperty(PROPERTY_PREFIX + name + ".max-attempts",
        Integer.class, defaultMaxAttempts);
    int windowMinutes = environment.getProperty(PROPERTY_PREFIX + name + ".window-minutes",
        Integer.class, defaultWindowMinutes);
    return new SplibAttemptLimiter(maxAttempts, Duration.ofMinutes(windowMinutes),
        SplibAttemptLimiter.DEFAULT_MAX_KEYS, clock);
  }

  /**
   * Returns whether the login code is enabled.
   *
   * @return {@code true} if the login code is enabled
   */
  public boolean isLoginCodeEnabled() {
    return loginCodeEnabled;
  }

  /**
   * Returns whether the session check is enabled.
   *
   * @return {@code true} if the session check is enabled
   */
  public boolean isSessionCheckEnabled() {
    return sessionCheckEnabled;
  }

  /**
   * Returns whether the attempt limit of wrong passwords is enabled.
   *
   * @return {@code true} if the attempt limit is enabled
   */
  public boolean isAttemptLimitEnabled() {
    return attemptLimitEnabled;
  }

  /**
   * Returns whether any of the features is enabled.
   *
   * @return {@code true} if any of the features is enabled
   */
  public boolean isAnyEnabled() {
    return loginCodeEnabled || sessionCheckEnabled || attemptLimitEnabled;
  }

  /**
   * Returns the url when the login procedure successfully ended.
   *
   * @return the url
   */
  public String getDefaultSuccessUrl() {
    return defaultSuccessUrl;
  }

  // ---------------------------------------------------------------------------------------------
  // called by apps
  // ---------------------------------------------------------------------------------------------

  /**
   * Records the current state of the logged-in admin to the session again.
   *
   * <p>Call it after the admin changes the own password, so that the session of the admin is not
   *     logged out by the session check. Does nothing unless the session check is enabled, or when
   *     the request is not of the admin who logged in on the admin login page.</p>
   *
   * @param request the request of the admin
   */
  public void recordSessionStamp(HttpServletRequest request) {
    if (!sessionCheckEnabled) {
      return;
    }

    String loginId = getLoggedInLoginId(request, currentAuthentication());
    if (loginId == null) {
      return;
    }

    SplibAdminAccountState state = requireAccountProvider().find(loginId);
    if (state != null) {
      request.getSession().setAttribute(SESSION_KEY_STAMP, stamp(state));
    }
  }

  /**
   * Removes the count of wrong passwords of {@code loginId}.
   *
   * <p>Call it after a password is set by a link sent by mail, since the owner of the account is
   *     verified by mail.</p>
   *
   * @param loginId the login ID
   */
  public void resetPasswordFailures(String loginId) {
    passwordFailureLimiter.reset(loginId);
  }

  // ---------------------------------------------------------------------------------------------
  // called by SplibWebSecurityConfigForAdmin
  // ---------------------------------------------------------------------------------------------

  /**
   * Creates the {@code AuthenticationSuccessHandler} of the admin login.
   *
   * <p>Used by {@code SplibWebSecurityConfigForAdmin} when any of the features is enabled.
   *     It records the state to the session, sends the login code, and redirects to the page to
   *     enter it when the login code is enabled, or to {@link #getDefaultSuccessUrl()}
   *     otherwise.</p>
   *
   * @return the handler
   */
  public AuthenticationSuccessHandler createLoginSuccessHandler() {
    SimpleUrlAuthenticationSuccessHandler defaultHandler =
        new SimpleUrlAuthenticationSuccessHandler(defaultSuccessUrl);
    defaultHandler.setAlwaysUseDefaultTargetUrl(true);

    return (request, response, authentication) -> {
      String loginId = authentication.getName();
      if (attemptLimitEnabled) {
        passwordFailureLimiter.reset(loginId);
      }

      HttpSession session = request.getSession();
      session.setAttribute(SESSION_KEY_LOGIN_ID, loginId);
      clearLoginCode(session);
      if (!loginCodeEnabled && !sessionCheckEnabled) {
        defaultHandler.onAuthenticationSuccess(request, response, authentication);
        return;
      }

      SplibAdminAccountState state = requireAccountProvider().find(loginId);
      if (state == null) {
        // UserDetailsService accepted the account, but SplibAdminAccountProvider did not.
        detailLog.warn("The admin account which logged in was not found by "
            + "SplibAdminAccountProvider, so the session is logged out.");
        logoutAndRedirect(request, response, authentication, ADMIN_LOGIN_PAGE_PATH + "?error");
        return;
      }

      if (sessionCheckEnabled) {
        session.setAttribute(SESSION_KEY_STAMP, stamp(state));
      }

      if (loginCodeEnabled) {
        try {
          issueLoginCode(request, state.mailAddress());

        } catch (Exception ex) {
          detailLog.error(ex, "Failed to send the login code.");
        }

        response.sendRedirect(request.getContextPath() + LOGIN_CODE_PAGE_PATH);
        return;
      }

      defaultHandler.onAuthenticationSuccess(request, response, authentication);
    };
  }

  /**
   * Creates the {@code AuthenticationFailureHandler} of the admin login.
   *
   * <p>Used by {@code SplibWebSecurityConfigForAdmin} when any of the features is enabled.
   *     It counts the wrong password when the attempt limit is enabled, and redirects to
   *     {@code failureUrl}.</p>
   *
   * @param failureUrl the url when the login failed
   * @return the handler
   */
  public AuthenticationFailureHandler createLoginFailureHandler(String failureUrl) {
    SimpleUrlAuthenticationFailureHandler defaultHandler =
        new SimpleUrlAuthenticationFailureHandler(failureUrl);

    return (request, response, exception) -> {
      String username = request.getParameter(USERNAME_PARAMETER);
      if (attemptLimitEnabled && exception instanceof BadCredentialsException
          && username != null) {
        // UsernamePasswordAuthenticationFilter trims the username too
        passwordFailureLimiter.record(username.trim());
      }

      defaultHandler.onAuthenticationFailure(request, response, exception);
    };
  }

  /**
   * Creates the {@code AuthenticationProvider} which rejects a login ID with too many wrong
   *     passwords, without checking the password.
   *
   * <p>Used by {@code SplibWebSecurityConfigForAdmin} when the attempt limit is enabled. It's
   *     added to the admin filter chain, and is asked before the app's
   *     {@code UserDetailsService}. It throws {@link LockedException}, which stops the
   *     authentication, publishes the failure event (recorded by the audit log), and shows the
   *     same page as other login failures. It does nothing (returns {@code null}) otherwise.</p>
   *
   * @return the provider
   */
  public AuthenticationProvider createAttemptLimitAuthenticationProvider() {
    return new AuthenticationProvider() {
      @Override
      public @Nullable Authentication authenticate(Authentication authentication) {
        if (passwordFailureLimiter.isExceeded(authentication.getName())) {
          throw new LockedException("Too many wrong passwords.");
        }

        return null;
      }

      @Override
      public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
      }
    };
  }

  /**
   * Creates the factory of {@code AuthorizationManager}s, which requires the login code
   *     ({@code FACTOR_OTT}) in addition.
   *
   * <p>Used by {@code SplibWebSecurityConfigForAdmin} for the roles and authorities of
   *     {@code /admin/**} when the login code is enabled. It's used only in the admin filter
   *     chain, not as a bean, so that other filter chains are not affected.</p>
   *
   * @return the factory
   */
  public DefaultAuthorizationManagerFactory<RequestAuthorizationContext>
      createAuthorizationManagerFactory() {
    return AuthorizationManagerFactories.<RequestAuthorizationContext>multiFactor()
        .requireFactors(FactorGrantedAuthority.OTT_AUTHORITY).build();
  }

  /**
   * Creates the {@code AccessDeniedHandler}, which redirects to the page to enter the login code
   *     when the login code is missing, and forwards to {@code accessDeniedPage} otherwise.
   *
   * <p>Used by {@code SplibWebSecurityConfigForAdmin} instead of
   *     {@code accessDeniedPage(accessDeniedPage)} when the login code is enabled.</p>
   *
   * @param accessDeniedPage the page when the access is denied
   * @return the handler
   */
  public AccessDeniedHandler createAccessDeniedHandler(String accessDeniedPage) {
    AccessDeniedHandlerImpl defaultHandler = new AccessDeniedHandlerImpl();
    defaultHandler.setErrorPage(accessDeniedPage);

    DelegatingMissingAuthorityAccessDeniedHandler handler =
        DelegatingMissingAuthorityAccessDeniedHandler.builder()
            .addEntryPointFor((request, response, ex) -> response
                .sendRedirect(request.getContextPath() + LOGIN_CODE_PAGE_PATH),
                FactorGrantedAuthority.OTT_AUTHORITY)
            .build();
    handler.setDefaultAccessDeniedHandler(defaultHandler);
    return handler;
  }

  /**
   * Creates the filter of the session check.
   *
   * <p>Registered by {@code SplibWebSecurityConfigForAdmin} after the Spring Security filters,
   *     and before {@code SwitchUserFilter}.</p>
   *
   * @return the filter
   */
  public Filter createSessionCheckFilter() {
    return new AdminSessionCheckFilter(this);
  }

  // ---------------------------------------------------------------------------------------------
  // called by the page to enter the login code
  // ---------------------------------------------------------------------------------------------

  /**
   * Returns the state of the login code of the request.
   *
   * @param request the request
   * @return the state
   */
  public LoginCodeState getLoginCodeState(HttpServletRequest request) {
    Authentication authentication = currentAuthentication();
    if (getLoggedInLoginId(request, authentication) == null) {
      return LoginCodeState.NOT_LOGGED_IN;
    }

    return hasLoginCodeFactor(Objects.requireNonNull(authentication)) ? LoginCodeState.VERIFIED
        : LoginCodeState.PENDING;
  }

  /**
   * Verifies the login code entered.
   *
   * <p>When it's right, {@code FACTOR_OTT} is added to the {@code Authentication}, and the session
   *     ID is changed (so that the session ID known while waiting for the code cannot be used).</p>
   *
   * @param request the request
   * @param response the response
   * @param input the code entered, may be {@code null}
   * @return the result
   */
  public VerifyResult verifyLoginCode(HttpServletRequest request, HttpServletResponse response,
      @Nullable String input) {
    Authentication authentication = currentAuthentication();
    String loginId = getPendingLoginId(request, authentication);
    if (loginId == null) {
      return VerifyResult.NOT_PENDING;
    }

    if (findAvailableAccount(request, loginId) == null) {
      return VerifyResult.ACCOUNT_UNAVAILABLE;
    }

    HttpSession session = request.getSession();
    if (codeFailureLimiter.isExceeded(loginId)) {
      clearLoginCode(session);
      return VerifyResult.TOO_MANY_FAILURES;
    }

    if (!(session.getAttribute(SESSION_KEY_CODE) instanceof String code)
        || !(session.getAttribute(SESSION_KEY_CODE_EXPIRES_AT) instanceof Instant expiresAt)
        || !(session.getAttribute(SESSION_KEY_CODE_FAILURES) instanceof Integer failures)
        || clock.instant().isAfter(expiresAt) || failures >= LOGIN_CODE_MAX_FAILURES) {
      clearLoginCode(session);
      return VerifyResult.UNUSABLE;
    }

    if (!MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8),
        (input == null ? "" : input.strip()).getBytes(StandardCharsets.UTF_8))) {
      session.setAttribute(SESSION_KEY_CODE_FAILURES, failures + 1);
      codeFailureLimiter.record(loginId);
      if (codeFailureLimiter.isExceeded(loginId)) {
        clearLoginCode(session);
        return VerifyResult.TOO_MANY_FAILURES;
      }

      return failures + 1 >= LOGIN_CODE_MAX_FAILURES ? VerifyResult.UNUSABLE : VerifyResult.WRONG;
    }

    clearLoginCode(session);
    codeFailureLimiter.reset(loginId);
    request.changeSessionId();

    Authentication verified = Objects.requireNonNull(authentication).toBuilder()
        .authorities(authorities -> authorities
            .add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.OTT_AUTHORITY)))
        .build();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(verified);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
    return VerifyResult.OK;
  }

  /**
   * Sends a new login code (the previous one cannot be used anymore).
   *
   * @param request the request
   * @return the result
   */
  public ResendResult resendLoginCode(HttpServletRequest request) {
    String loginId = getPendingLoginId(request, currentAuthentication());
    if (loginId == null) {
      return ResendResult.NOT_PENDING;
    }

    SplibAdminAccountState state = findAvailableAccount(request, loginId);
    if (state == null) {
      return ResendResult.ACCOUNT_UNAVAILABLE;
    }

    if (codeResendLimiter.isExceeded(loginId)) {
      return ResendResult.LIMIT_EXCEEDED;
    }

    codeResendLimiter.record(loginId);
    try {
      issueLoginCode(request, state.mailAddress());

    } catch (Exception ex) {
      detailLog.error(ex, "Failed to send the login code.");
      return ResendResult.SEND_FAILED;
    }

    return ResendResult.SENT;
  }

  /**
   * Logs out the session.
   *
   * @param request the request
   * @param response the response
   */
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    new SecurityContextLogoutHandler().logout(request, response, currentAuthentication());
  }

  // ---------------------------------------------------------------------------------------------
  // called by AdminSessionCheckFilter
  // ---------------------------------------------------------------------------------------------

  /**
   * Returns whether the session of the admin of {@code loginId} can still be used.
   */
  boolean isSessionValid(HttpServletRequest request, String loginId) {
    HttpSession session = request.getSession(false);
    if (session == null || !loginId.equals(session.getAttribute(SESSION_KEY_LOGIN_ID))) {
      return false;
    }

    return findAvailableAccount(request, loginId) != null;
  }

  void logoutAndRedirect(HttpServletRequest request, HttpServletResponse response,
      @Nullable Authentication authentication, String path) throws IOException {
    new SecurityContextLogoutHandler().logout(request, response, authentication);
    response.sendRedirect(request.getContextPath() + path);
  }

  // ---------------------------------------------------------------------------------------------
  // internal
  // ---------------------------------------------------------------------------------------------

  /**
   * Returns the state of the account when it can log in and, with the session check, its stamp
   *     has not changed since the login.
   */
  private @Nullable SplibAdminAccountState findAvailableAccount(HttpServletRequest request,
      String loginId) {
    SplibAdminAccountState state = requireAccountProvider().find(loginId);
    if (state == null) {
      return null;
    }

    if (sessionCheckEnabled) {
      HttpSession session = request.getSession(false);
      if (session == null || !stamp(state).equals(session.getAttribute(SESSION_KEY_STAMP))) {
        return null;
      }
    }

    return state;
  }

  /**
   * Returns the login ID when the request is of the admin who logged in on the admin login page
   *     (not while switched to another user).
   */
  private static @Nullable String getLoggedInLoginId(HttpServletRequest request,
      @Nullable Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken
        || SwitchUserUtil.isSwitchedUser(authentication)) {
      return null;
    }

    HttpSession session = request.getSession(false);
    if (session == null
        || !authentication.getName().equals(session.getAttribute(SESSION_KEY_LOGIN_ID))) {
      return null;
    }

    return authentication.getName();
  }

  private @Nullable String getPendingLoginId(HttpServletRequest request,
      @Nullable Authentication authentication) {
    String loginId = getLoggedInLoginId(request, authentication);
    if (loginId == null || hasLoginCodeFactor(Objects.requireNonNull(authentication))) {
      return null;
    }

    return loginId;
  }

  private static boolean hasLoginCodeFactor(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(a -> FactorGrantedAuthority.OTT_AUTHORITY.equals(a.getAuthority()));
  }

  private void issueLoginCode(HttpServletRequest request, String mailAddress) throws Exception {
    String code = String.format("%06d", random.nextInt(1_000_000));
    HttpSession session = request.getSession();
    session.setAttribute(SESSION_KEY_CODE, code);
    session.setAttribute(SESSION_KEY_CODE_EXPIRES_AT,
        clock.instant().plus(LOGIN_CODE_VALID_DURATION));
    session.setAttribute(SESSION_KEY_CODE_FAILURES, 0);

    Locale locale = LocaleContextHolder.getLocale();
    String title = PropertiesFileUtil.getMessage(locale, MSG_MAIL_TITLE);
    String body = PropertiesFileUtil.getMessage(locale, MSG_MAIL_BODY, code,
        LOGIN_CODE_VALID_DURATION.toMinutes());
    Objects.requireNonNull(mailUtil).sendTextMail(List.of(mailAddress), null, title, body);
  }

  private static void clearLoginCode(HttpSession session) {
    session.removeAttribute(SESSION_KEY_CODE);
    session.removeAttribute(SESSION_KEY_CODE_EXPIRES_AT);
    session.removeAttribute(SESSION_KEY_CODE_FAILURES);
  }

  private SplibAdminAccountProvider requireAccountProvider() {
    return Objects.requireNonNull(accountProvider);
  }

  private static @Nullable Authentication currentAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  private static String stamp(SplibAdminAccountState state) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(state.stampSource().getBytes(StandardCharsets.UTF_8)));

    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
