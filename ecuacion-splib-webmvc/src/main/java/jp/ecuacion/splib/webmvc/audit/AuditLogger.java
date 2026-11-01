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
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.splib.core.audit.AuditLogContext;
import jp.ecuacion.splib.core.audit.AuditLogWriter;
import jp.ecuacion.splib.core.audit.AuditLogWriter.Entry;
import jp.ecuacion.splib.core.audit.enums.AuditLogEventType;
import jp.ecuacion.splib.core.audit.enums.AuditLogResult;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;

/**
 * Builds audit log records and passes them to {@link AuditLogWriter}.
 *
 * <p>Registered as a bean by {@link AuditLogConfig} only when the audit log is enabled.
 *     {@link AuditLogInterceptor} and {@link AuditLogSecurityEventListener} use it;
 *     {@link #recordLoginFailure} is public for {@code ecuacion-splib-rest}'s {@code X-Api-Key}
 *     authentication filters.</p>
 *
 * <p>Never throws: an exception while building or writing a record is logged as an error, so
 *     that a problem in the audit log does not make the operation or login being recorded fail.
 *     </p>
 */
public class AuditLogger {

  /** The number of trailing characters of the session ID recorded. */
  private static final int SESSION_ID_SUFFIX_LENGTH = 8;

  private final DetailLogger detailLog = new DetailLogger(this);

  private final AuditLogWriter writer;

  /**
   * Constructs a new instance.
   *
   * @param writer the writer
   */
  public AuditLogger(AuditLogWriter writer) {
    this.writer = writer;
  }

  /**
   * Records a failed login with the username as typed.
   *
   * <p>Used for authentications that do not go through Spring Security's
   *     {@code AuthenticationManager} and so publish no event, such as
   *     {@code ecuacion-splib-rest}'s {@code X-Api-Key} authentication filters.</p>
   *
   * @param request the request of the login attempt
   * @param attemptedName the username or key ID as presented, may be {@code null} when none was
   *     presented
   * @param reason why it was rejected; do not include the presented secret
   */
  public void recordLoginFailure(HttpServletRequest request, @Nullable String attemptedName,
      String reason) {
    record(AuditLogEventType.LOGIN, AuditLogResult.FAILURE, request, attemptedName,
        null, null, null, reason);
  }

  /**
   * Records an operation, a request handled by a controller method.
   */
  void recordOperation(HttpServletRequest request, @Nullable Authentication authentication,
      String handler, AuditLogResult result, @Nullable Throwable exception,
      @Nullable AuditLogContext context) {
    record(AuditLogEventType.OPERATION, result, request, actorOf(authentication),
        originalActorOf(authentication), handler, context, errorOf(exception));
  }

  /**
   * Records a login, logout or switch-user event of the logged-in user {@code authentication}.
   */
  void recordAuthenticationEvent(AuditLogEventType eventType, AuditLogResult result,
      @Nullable HttpServletRequest request, @Nullable Authentication authentication) {
    record(eventType, result, request, actorOf(authentication),
        originalActorOf(authentication), null, null, null);
  }

  /**
   * Records the start of a switch user, the same way as the operations that follow it: the
   * switched user as {@code actor} and the administrator as {@code originalActor}.
   */
  void recordSwitchUserStart(@Nullable HttpServletRequest request, Authentication switched,
      Authentication administrator) {
    record(AuditLogEventType.SWITCH_USER_START, AuditLogResult.SUCCESS, request,
        actorOf(switched), actorOf(administrator), null, null, null);
  }

  /**
   * Builds a record and passes it to the writer.
   */
  void record(AuditLogEventType eventType, AuditLogResult result,
      @Nullable HttpServletRequest request, @Nullable String actor,
      @Nullable String originalActor, @Nullable String handler,
      @Nullable AuditLogContext context, @Nullable String error) {
    try {
      // The server's own time zone, like the other log files.
      Entry record = new Entry(
          OffsetDateTime.now(ZoneId.systemDefault()),
          context == null ? null : context.getOperationId(), eventType, result, actor,
          originalActor,
          detailsOf(request, handler, context),
          context == null ? List.of() : context.getTargets(), error,
          context == null ? Map.of() : context.getAttributes());

      writer.write(record);
    } catch (RuntimeException ex) {
      LogUtil.logSystemError(detailLog, ex);
    }
  }

  /**
   * Returns the value recorded for {@code authentication}, {@code null} if not logged in.
   */
  static @Nullable String actorOf(@Nullable Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }

    return authentication.getName();
  }

  /**
   * Returns the value recorded for the administrator behind a switch user, {@code null} if
   * {@code authentication} is not a switched user.
   */
  static @Nullable String originalActorOf(@Nullable Authentication authentication) {
    Authentication source = switchUserSourceOf(authentication);
    return source == null ? null : actorOf(source);
  }

  /**
   * Returns the {@code Authentication} from before the switch user, {@code null} if
   * {@code authentication} is not a switched user.
   */
  static @Nullable Authentication switchUserSourceOf(@Nullable Authentication authentication) {
    if (authentication == null) {
      return null;
    }

    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (authority instanceof SwitchUserGrantedAuthority switchUserAuthority) {
        return switchUserAuthority.getSource();
      }
    }

    return null;
  }

  /**
   * Returns the items of a Spring MVC request: always the same five, {@code null} when not
   * known, followed by those added to {@code context} (e.g. {@code ecuacion-splib-web}'s
   * {@code loginState}).
   */
  private static Map<String, @Nullable String> detailsOf(@Nullable HttpServletRequest request,
      @Nullable String handler, @Nullable AuditLogContext context) {
    Map<String, @Nullable String> details = new LinkedHashMap<>();
    details.put("clientIp", request == null ? null : request.getRemoteAddr());
    details.put("sessionIdSuffix", request == null ? null : sessionIdSuffixOf(request));
    details.put("httpMethod", request == null ? null : request.getMethod());
    details.put("uri", request == null ? null : request.getRequestURI());
    details.put("handler", handler);
    if (context != null) {
      details.putAll(context.getDetails());
    }

    return details;
  }

  private static @Nullable String sessionIdSuffixOf(HttpServletRequest request) {
    // getSession(false): recording must not create a session (e.g. for stateless REST calls).
    HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }

    String id = session.getId();
    return id.length() > SESSION_ID_SUFFIX_LENGTH
        ? id.substring(id.length() - SESSION_ID_SUFFIX_LENGTH)
        : id;
  }

  /**
   * Returns the exception class name, followed by the message IDs for a
   * {@code ViolationException} / {@code ConstraintViolationException}: e.g.
   * {@code "jp.ecuacion.lib.core.exception.ViolationException [MSG_ID_1, MSG_ID_2]"}. The rendered
   * message is not included since it may contain values the user typed in.
   */
  static @Nullable String errorOf(@Nullable Throwable exception) {
    if (exception == null) {
      return null;
    }

    List<String> messageIds = new ArrayList<>();
    if (exception instanceof ViolationException ve) {
      for (ConstraintViolation<?> cv : ve.getViolations().getConstraintViolations()) {
        messageIds.add(messageIdOf(cv));
      }
      for (BusinessViolation bv : ve.getViolations().getBusinessViolations()) {
        messageIds.add(bv.getMessageId());
      }
    } else if (exception instanceof ConstraintViolationException cve) {
      for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
        messageIds.add(messageIdOf(cv));
      }
    }

    String className = exception.getClass().getName();
    return messageIds.isEmpty() ? className : className + " " + messageIds;
  }

  private static String messageIdOf(ConstraintViolation<?> cv) {
    return cv.getMessageTemplate().replace("{", "").replace("}", "");
  }
}
