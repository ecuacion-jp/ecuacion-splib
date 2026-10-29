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

import jakarta.servlet.http.HttpServletRequest;
import jp.ecuacion.splib.core.audit.enums.AuditLogEventType;
import jp.ecuacion.splib.core.audit.enums.AuditLogResult;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Records login, login failure, logout and switch-user events in the audit log, from the events
 * Spring Security publishes.
 *
 * <ul>
 *   <li>{@link AuthenticationSuccessEvent} / {@link AbstractAuthenticationFailureEvent}: published
 *       by the {@code AuthenticationManager} for form login, OAuth2 login and remember-me.
 *       {@code ecuacion-splib-rest}'s {@code X-Api-Key} authentication does not use it and
 *       records its failures itself (see {@link AuditLogger#recordLoginFailure}); its
 *       successes are not recorded, since every API call authenticates and the call itself is
 *       recorded as an operation.</li>
 *   <li>{@link LogoutSuccessEvent}: published by the logout filter.</li>
 *   <li>{@link AuthenticationSwitchUserEvent}: published by {@code SwitchUserFilter} both when
 *       switching and when exiting.</li>
 * </ul>
 *
 * <p>Like the records of operations made during a switch user, a switch-user record has the
 *     switched user as {@code actor} and the administrator as {@code originalActor}, both when
 *     starting and when exiting.</p>
 *
 * <p>These events are published on the thread handling the request, so the request is taken
 *     from {@link RequestContextHolder}.</p>
 */
public class AuditLogSecurityEventListener {

  private final AuditLogger auditLogger;

  /**
   * Constructs a new instance with {@code auditLogger}.
   *
   * @param auditLogger the logger records are passed to
   */
  public AuditLogSecurityEventListener(AuditLogger auditLogger) {
    this.auditLogger = auditLogger;
  }

  /**
   * Records a successful login.
   *
   * @param event the event
   */
  @EventListener
  public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
    auditLogger.recordAuthenticationEvent(AuditLogEventType.LOGIN,
        AuditLogResult.SUCCESS, currentRequest(), event.getAuthentication());
  }

  /**
   * Records a failed login with the username as typed.
   *
   * @param event the event
   */
  @EventListener
  public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    auditLogger.record(AuditLogEventType.LOGIN, AuditLogResult.FAILURE,
        currentRequest(), event.getAuthentication().getName(), null, null, null,
        event.getException().getClass().getName());
  }

  /**
   * Records a logout.
   *
   * @param event the event
   */
  @EventListener
  public void onLogoutSuccess(LogoutSuccessEvent event) {
    auditLogger.recordAuthenticationEvent(AuditLogEventType.LOGOUT,
        AuditLogResult.SUCCESS, currentRequest(), event.getAuthentication());
  }

  /**
   * Records the start or the end of a switch user.
   *
   * <p>{@code SwitchUserFilter} publishes the same event for both: on switching, with the
   *     administrator as the authentication and the switched user as the target; on exiting, with
   *     the switched user as the authentication and the administrator as the target. It is an exit
   *     when the authentication is a switched user whose source is the target.</p>
   *
   * <p>{@code SwitchUserFilter} always sets the target, but the event allows none. In that case
   *     it is taken as an exit if the authentication is a switched user, and the record has only
   *     the authentication's users.</p>
   *
   * @param event the event
   */
  @EventListener
  public void onSwitchUser(AuthenticationSwitchUserEvent event) {
    Authentication current = event.getAuthentication();
    UserDetails target = event.getTargetUser();
    Authentication source = AuditLogger.switchUserSourceOf(current);
    HttpServletRequest request = currentRequest();

    if (target == null) {
      auditLogger.recordAuthenticationEvent(source != null
          ? AuditLogEventType.SWITCH_USER_EXIT
          : AuditLogEventType.SWITCH_USER_START, AuditLogResult.SUCCESS, request,
          current);

    } else if (source != null && source.getName().equals(target.getUsername())) {
      auditLogger.recordAuthenticationEvent(AuditLogEventType.SWITCH_USER_EXIT,
          AuditLogResult.SUCCESS, request, current);

    } else {
      auditLogger.recordSwitchUserStart(request, UsernamePasswordAuthenticationToken
          .authenticated(target, null, target.getAuthorities()), current);
    }
  }

  private static @Nullable HttpServletRequest currentRequest() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
        ? attrs.getRequest()
        : null;
  }
}
