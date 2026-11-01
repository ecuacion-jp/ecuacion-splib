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
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.core.annotation.ReadOnlyOperation;
import jp.ecuacion.splib.web.config.SplibWebSecurityConfigForSwitchUser;
import jp.ecuacion.splib.webmvc.util.ReadOnlyOperationUtil;
import jp.ecuacion.splib.webmvc.util.SwitchUserUtil;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rejects the requests that change something while switched to another user.
 *
 * <p>Registered by {@link SplibWebSecurityConfigForSwitchUser} when
 *     {@link SplibWebSecurityConfigForSwitchUser#isReadOnlyWhileSwitched()} returns
 *     {@code true}. While not switched, it does nothing.</p>
 *
 * <p><strong>Which requests.</strong> {@code REQUEST} dispatches to a controller method
 *     ({@link HandlerMethod}) that change something, decided by
 *     {@link ReadOnlyOperationUtil#isReadOnly} (by default, those whose HTTP method is not
 *     {@code GET}, {@code HEAD}, {@code OPTIONS} or {@code TRACE}, unless
 *     {@link ReadOnlyOperation} says otherwise) — the same way as the audit log decides which
 *     operations to record. Exiting the switched user ({@code POST /account/exitUser}) and
 *     logging out are processed by Spring Security's filters before reaching here, so they are
 *     never rejected.</p>
 *
 * <p><strong>How.</strong> Throws a {@link ViolationException} with the message
 *     {@value #MSG_ID}, which {@code SplibWebExceptionHandler} shows on the page the request
 *     was sent from. The audit log records the rejected operation as a business error.</p>
 */
public class SwitchUserReadOnlyInterceptor implements HandlerInterceptor {

  /** The message ID of the message shown when a request is rejected. */
  public static final String MSG_ID = "jp.ecuacion.splib.web.switchUser.readOnly";

  /**
   * Constructs a new instance.
   */
  public SwitchUserReadOnlyInterceptor() {}

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (request.getDispatcherType() == DispatcherType.REQUEST
        && handler instanceof HandlerMethod handlerMethod
        && !ReadOnlyOperationUtil.isReadOnly(request, handlerMethod)
        && SwitchUserUtil.isSwitchedUser()) {
      throw new ViolationException(new Violations().add(new BusinessViolation(MSG_ID)));
    }

    return true;
  }
}
