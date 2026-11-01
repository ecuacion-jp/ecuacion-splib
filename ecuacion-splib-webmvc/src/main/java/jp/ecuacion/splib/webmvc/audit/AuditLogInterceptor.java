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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import java.util.Set;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.splib.core.audit.AuditLogContext;
import jp.ecuacion.splib.core.audit.AuditLogTarget;
import jp.ecuacion.splib.core.audit.AuditLogUtil;
import jp.ecuacion.splib.core.audit.enums.AuditLogResult;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Records requests to controller methods in the audit log, one record per request.
 *
 * <p><strong>Which requests.</strong> Requests whose handler is a controller method
 *     ({@link HandlerMethod}) and whose HTTP method is not {@code GET}, {@code HEAD},
 *     {@code OPTIONS} or {@code TRACE}, unless {@link AuditLogTarget} says otherwise. Only
 *     {@code REQUEST} and {@code ASYNC} dispatches are looked at: the {@code ERROR} dispatch that
 *     renders the error page, and {@code FORWARD} / {@code INCLUDE}, belong to a request already
 *     recorded. When the handler starts asynchronous processing, the record is written once, at
 *     the end of the {@code ASYNC} dispatch that completes it. What {@link AuditLogUtil} collected
 *     before the asynchronous processing started is not taken over (splib has no asynchronous
 *     controllers; add it when one is needed).</p>
 *
 * <p><strong>Data change history.</strong> For every request to a controller method, recorded
 *     or not, it keeps who does the operation and the operation's ID ({@link AuditLogContext})
 *     during the request, which {@code ecuacion-splib-jpa}'s data change history records with
 *     each change. With only the data change history enabled, it records nothing itself.</p>
 *
 * <p><strong>Result.</strong> Decided in {@link #resultOf}. An exception handled by an
 *     {@code @ExceptionHandler} (e.g. a {@code ViolationException} that
 *     {@code SplibWebExceptionHandler} turns into a redirect back to the form) never reaches
 *     {@link #afterCompletion}, and {@code DispatcherServlet} clears the request attributes it
 *     exposes the exception under once the error view is rendered — before
 *     {@link #afterCompletion} runs. So {@link #exceptionRecorder()} provides a
 *     {@link HandlerExceptionResolver}, registered ahead of every other resolver by
 *     {@link AuditLogConfig}, that only remembers the exception and leaves the handling to the
 *     resolvers after it.</p>
 */
public class AuditLogInterceptor implements AsyncHandlerInterceptor {

  /** Request attribute holding the exception the handler threw. */
  private static final String ATTR_EXCEPTION = AuditLogInterceptor.class.getName() + ".EXCEPTION";

  private static final Set<String> NOT_RECORDED_BY_DEFAULT =
      Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  private final @Nullable AuditLogger auditLogger;

  /**
   * Constructs a new instance with {@code auditLogger}.
   *
   * @param auditLogger the logger records are passed to, {@code null} when the audit log is
   *     disabled and only the data change history is enabled
   */
  public AuditLogInterceptor(@Nullable AuditLogger auditLogger) {
    this.auditLogger = auditLogger;
  }

  /**
   * Starts collecting for every request to a controller method, recorded or not, so that the
   * data change history knows who changed the data even in a request the audit log does not
   * record (e.g. a {@code POST} with {@code @AuditLogTarget(false)}).
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (isOperation(request, handler)) {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      AuditLogContext.begin(AuditLogger.actorOf(authentication),
          AuditLogger.originalActorOf(authentication));
    }

    return true;
  }

  /**
   * Clears what was collected so far, so that it is not left on the thread released to the pool;
   * the {@code ASYNC} dispatch that completes the request starts collecting again.
   */
  @Override
  public void afterConcurrentHandlingStarted(HttpServletRequest request,
      HttpServletResponse response, Object handler) throws Exception {
    AuditLogContext.end();
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, @Nullable Exception ex) throws Exception {
    if (!isOperation(request, handler)) {
      return;
    }

    AuditLogContext context = AuditLogContext.end();
    if (auditLogger == null || !isRecorded(request, handler)) {
      return;
    }

    Throwable handled = request.getAttribute(ATTR_EXCEPTION) instanceof Throwable t ? t : null;
    Throwable exception = ex != null ? ex : handled;

    HandlerMethod handlerMethod = (HandlerMethod) handler;
    Objects.requireNonNull(auditLogger).recordOperation(request,
        SecurityContextHolder.getContext().getAuthentication(),
        handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName(),
        resultOf(ex, handled, response.getStatus()), exception, context);
  }

  /**
   * Returns whether the request is an operation: a {@code REQUEST} or {@code ASYNC} dispatch to a
   * controller method. See the class Javadoc.
   */
  static boolean isOperation(HttpServletRequest request, Object handler) {
    DispatcherType dispatcherType = request.getDispatcherType();
    return (dispatcherType == DispatcherType.REQUEST || dispatcherType == DispatcherType.ASYNC)
        && handler instanceof HandlerMethod;
  }

  /**
   * Returns whether the request is recorded in the audit log. See the class Javadoc.
   */
  static boolean isRecorded(HttpServletRequest request, Object handler) {
    if (!isOperation(request, handler)) {
      return false;
    }

    HandlerMethod handlerMethod = (HandlerMethod) handler;

    AuditLogTarget annotation = handlerMethod.getMethodAnnotation(AuditLogTarget.class);
    if (annotation == null) {
      annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(),
          AuditLogTarget.class);
    }

    return annotation != null ? annotation.value()
        : !NOT_RECORDED_BY_DEFAULT.contains(request.getMethod());
  }

  /**
   * Decides the result.
   *
   * <ul>
   *   <li>An exception no {@code @ExceptionHandler} handled ({@code unhandled}): system error.</li>
   *   <li>A handled {@code ViolationException} (including {@code ViolationWarningException}) or
   *       {@code ConstraintViolationException}: business error.</li>
   *   <li>Another handled exception: system error if the response status is 5xx, otherwise
   *       business error (e.g. {@code SplibWebExceptionHandler} redirects with a message for an
   *       optimistic lock failure or too large an upload, but answers 500 for any other
   *       exception).</li>
   *   <li>No exception: system error if 5xx, business error if 4xx, success otherwise.</li>
   * </ul>
   */
  static AuditLogResult resultOf(@Nullable Throwable unhandled, @Nullable Throwable handled,
      int status) {
    if (unhandled != null) {
      return AuditLogResult.SYSTEM_ERROR;
    }

    if (handled instanceof ViolationException || handled instanceof ConstraintViolationException) {
      return AuditLogResult.BUSINESS_ERROR;
    }

    if (status >= 500) {
      return AuditLogResult.SYSTEM_ERROR;
    }

    return handled != null || status >= 400 ? AuditLogResult.BUSINESS_ERROR
        : AuditLogResult.SUCCESS;
  }

  /**
   * Returns a {@link HandlerExceptionResolver} that only remembers the exception for
   * {@link #afterCompletion}, returning {@code null} so that the resolvers after it handle it.
   *
   * @return the resolver
   */
  public static HandlerExceptionResolver exceptionRecorder() {
    return new ExceptionRecorder();
  }

  private static class ExceptionRecorder implements HandlerExceptionResolver {

    @Override
    public @Nullable ModelAndView resolveException(HttpServletRequest request,
        HttpServletResponse response, @Nullable Object handler, Exception ex) {
      request.setAttribute(ATTR_EXCEPTION, ex);
      return null;
    }
  }
}
