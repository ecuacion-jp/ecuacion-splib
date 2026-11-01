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
package jp.ecuacion.splib.webmvc.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import jp.ecuacion.splib.core.annotation.ReadOnlyOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

/**
 * Provides utility methods on whether a request to a controller method changes nothing
 *     ({@link ReadOnlyOperation}).
 *
 * <p>Used by the audit log (an operation that changes nothing is not recorded by default) and by
 *     {@code ecuacion-splib-web}'s switched user (only operations that change nothing are
 *     allowed while switched, when so configured), so that both decide the same way.</p>
 */
public class ReadOnlyOperationUtil {

  private static final Set<String> READ_ONLY_BY_DEFAULT =
      Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  private ReadOnlyOperationUtil() {}

  /**
   * Returns whether the request to {@code handlerMethod} changes nothing.
   *
   * <p>Decided by {@link ReadOnlyOperation} on the method (including the method it overrides),
   *     then on the class. Without it, a request with the HTTP method {@code GET},
   *     {@code HEAD}, {@code OPTIONS} or {@code TRACE} changes nothing.</p>
   *
   * @param request the request
   * @param handlerMethod the controller method handling the request
   * @return {@code true} if the request changes nothing
   */
  public static boolean isReadOnly(HttpServletRequest request, HandlerMethod handlerMethod) {
    ReadOnlyOperation annotation = handlerMethod.getMethodAnnotation(ReadOnlyOperation.class);
    if (annotation == null) {
      annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(),
          ReadOnlyOperation.class);
    }

    return annotation != null ? annotation.value()
        : READ_ONLY_BY_DEFAULT.contains(request.getMethod());
  }
}
