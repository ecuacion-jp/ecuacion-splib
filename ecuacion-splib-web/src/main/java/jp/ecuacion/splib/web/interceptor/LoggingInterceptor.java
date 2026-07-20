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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.StringUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Offers logging features.
 */
public class LoggingInterceptor implements HandlerInterceptor {

  /** Marker string logged in place of the value of any parameter whose name looks sensitive. */
  private static final String MASKED_VALUE = "***";

  private final DetailLogger detailLog = new DetailLogger(this);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    List<String> paramList = request.getParameterMap().entrySet().stream()
        .map(e -> e.getKey() + "=" + formatParamValue(e.getKey(), e.getValue())).toList();

    detailLog.debug(getPrefix(request) + "request process started. request: "
        + request.getRequestURI() + (paramList.isEmpty() ? ""
            : ", parameters: " + StringUtil.getSeparatedValuesString(paramList, "&")));

    return true;
  }

  /**
   * Returns the CSV-joined {@code values}, or {@link #MASKED_VALUE} if {@code key} looks like it
   * carries a credential (e.g. {@code login.password}, {@code adminLogin.password} — every
   * password field in this library and, by convention, in applications built on it names itself
   * with "password" somewhere in the key). Request parameters are entirely client-supplied and
   * this log line is written at DEBUG for every request, so without this check any login or
   * password-change submission would place the raw password in the log file.
   */
  private String formatParamValue(String key, String[] values) {
    return key.toLowerCase(Locale.ROOT).contains("password") ? MASKED_VALUE
        : StringUtil.getCsv(values);
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      @Nullable ModelAndView modelAndView) throws Exception {
    detailLog.debug(getPrefix(request) + "request process finished.");
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, @Nullable Exception ex) throws Exception {
    detailLog.debug(getPrefix(request) + "view rendering finished.");
  }

  /**
   * Only the last 8 characters of the session ID are logged. The full ID is a bearer credential
   * for the session (anyone who obtains it can hijack the session without a password), and this
   * prefix is written at DEBUG on every request; the suffix is still enough to correlate the
   * lines belonging to one session in the log.
   */
  private String getPrefix(HttpServletRequest request) {
    String sessionId = request.getSession().getId();
    String sessionIdSuffix =
        sessionId.length() > 8 ? sessionId.substring(sessionId.length() - 8) : sessionId;
    return "session ID (last 8 chars): " + sessionIdSuffix + ", thread ID: "
        + Thread.currentThread().threadId() + " : ";
  }
}
