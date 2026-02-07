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
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.StringUtil;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Offers logging features.
 */
public class LoggingInterceptor implements HandlerInterceptor {

  private final DetailLogger detailLog = new DetailLogger(this);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    List<String> paramList = request.getParameterMap().entrySet().stream()
        .map(e -> e.getKey() + "=" + StringUtil.getCsv(e.getValue())).toList();

    detailLog.debug(getPrefix(request) + "request process started. request: "
        + request.getRequestURI() + (paramList.size() == 0 ? ""
            : ", parameters: " + StringUtil.getSeparatedValuesString(paramList, "&")));

    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      @Nullable ModelAndView modelAndView) throws Exception {
    detailLog.debug(getPrefix(request) + "request process finished.");
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) throws Exception {
    detailLog.debug(getPrefix(request) + "view rendering finished.");
  }

  private String getPrefix(HttpServletRequest request) {
    return "session ID: " + request.getSession().getId() + ", thread ID: "
        + Thread.currentThread().threadId() + " : ";
  }
}
