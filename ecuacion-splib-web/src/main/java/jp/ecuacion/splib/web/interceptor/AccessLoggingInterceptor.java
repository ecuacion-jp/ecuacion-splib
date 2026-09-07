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
package jp.ecuacion.splib.web.interceptor;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.jspecify.annotations.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Logs accesses to controller methods at INFO level.
 *
 * <p>Unlike {@link LoggingInterceptor}, which logs every request (including static resources
 *     such as images or css) at TRACE level, this interceptor only logs requests actually
 *     dispatched to a controller method, identified by {@code handler} being a
 *     {@link HandlerMethod}.</p>
 */
public class AccessLoggingInterceptor implements AsyncHandlerInterceptor {

  private final DetailLogger detailLog = new DetailLogger(this);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
      Object handler) throws Exception {
    if (handler instanceof HandlerMethod handlerMethod) {
      detailLog.info(getMessage(request, handlerMethod) + " : started.");
    }

    return true;
  }

  /**
   * Logs at {@code afterCompletion} rather than {@code postHandle} so the log line is always
   * written even when the controller method (or view rendering) threw — {@code postHandle} is
   * skipped in that case.
   */
  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, @Nullable Exception ex) throws Exception {
    if (handler instanceof HandlerMethod handlerMethod) {
      detailLog.info(getMessage(request, handlerMethod) + " : finished."
          + (ex == null ? "" : " (exception occurred: " + ex.getClass().getName() + ")"));
    }
  }

  /**
   * Called instead of {@code postHandle}/{@code afterCompletion} when the handler starts
   * asynchronous processing (e.g. it returns a {@code StreamingResponseBody}). In that case the
   * servlet container hands the rest of the request off to another thread as a separate
   * {@code ASYNC} dispatch, which re-enters this interceptor and logs its own
   * {@code started}/{@code finished} pair. Without this log line, that second pair looks like an
   * unrelated duplicate request rather than a continuation of this one.
   */
  @Override
  public void afterConcurrentHandlingStarted(HttpServletRequest request,
      HttpServletResponse response, Object handler) throws Exception {
    if (handler instanceof HandlerMethod handlerMethod) {
      detailLog.info(getMessage(request, handlerMethod)
          + " : handed off to async processing; will resume as a separate ASYNC dispatch, "
          + "logged again below as its own started/finished pair.");
    }
  }

  /**
   * A non-empty input flash map means the previous request carried data over for this one via
   * {@code RedirectAttributes} (e.g. {@code SplibWebExceptionHandler}'s redirect-with-violations
   * flow, or an app's own Post-Redirect-Get save flow) — i.e. this request is the "Get" that
   * followed a server-issued redirect, not a fresh navigation.
   *
   * <p>A {@link DispatcherType#ASYNC} dispatch means this call is the container-internal
   *     resumption of a request whose handler previously returned from {@link #preHandle} via
   *     {@link #afterConcurrentHandlingStarted} to start asynchronous processing (e.g. it
   *     returned a {@code StreamingResponseBody}) — not a separate request from the browser.</p>
   */
  private String getMessage(HttpServletRequest request, HandlerMethod handlerMethod) {
    Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
    boolean isRedirected = inputFlashMap != null && !inputFlashMap.isEmpty();
    boolean isAsync = request.getDispatcherType() == DispatcherType.ASYNC;

    return (isRedirected ? "(redirected) " : "") + (isAsync ? "(async) " : "")
        + request.getMethod() + " " + request.getRequestURI() + " -> "
        + handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
  }
}
