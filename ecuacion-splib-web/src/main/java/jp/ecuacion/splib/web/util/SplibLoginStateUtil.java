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
package jp.ecuacion.splib.web.util;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import jp.ecuacion.splib.web.constant.LoginState;
import org.springframework.stereotype.Component;

/**
 * Resolves the login state from the current request URL.
 */
@Component
public class SplibLoginStateUtil {

  private HttpServletRequest request;

  private ServletContext servletContext;

  /**
   * Constructs a new instance.
   *
   * @param request request
   * @param servletContext servletContext
   */
  public SplibLoginStateUtil(HttpServletRequest request, ServletContext servletContext) {
    this.request = request;
    this.servletContext = servletContext;
  }

  /**
   * Gets loginState.
   *
   * @return String
   */
  public String getLoginState() {
    String urlPathWithContextPath = request.getRequestURI();
    String contextPath = servletContext.getContextPath();

    // From testing, servletContext.getContextPath() (A) has a leading "/" like "/contextPath",
    // and request.getRequestURI() (B) is like "/contextPath/public/...",
    // so B.replaceFirst(A) is expected to cleanly remove the contextPath prefix,
    // resulting in "/public/..." just as when there is no contextPath.
    // However, to guard against irregularities such as getContextPath() returning a path
    // without a leading "/", add a check here as a precaution.
    if ((!contextPath.isEmpty() && !contextPath.startsWith("/"))
        || !urlPathWithContextPath.startsWith("/")) {
      throw new RuntimeException(
          "servletContext.getContextPath() or request.getRequestURI() does not start with"
              + " \"/\". Unexpected. servletContext.getContextPath() = " + contextPath
              + ", request.getRequestURI() = " + urlPathWithContextPath);
    }

    String urlPath = urlPathWithContextPath;

    // In addition to contextPath == "", also handle contextPath == "/"; ignore both cases.
    if (!contextPath.isEmpty() && !contextPath.equals("/")) {
      urlPath = urlPath.replaceFirst(servletContext.getContextPath(), "");
    }

    String tmp = urlPath.substring(1);
    String loginState = tmp.substring(0, tmp.indexOf("/"));

    // If loginState is "ecuacion", also append the next path segment to loginState.
    // e.g. /ecuacion/public/... -> "ecuacion-public"
    if (loginState.equals("ecuacion")) {
      // At this point tmp is in the state "ecuacion/public/..." with the leading "/" removed.
      // Extract "public" from it.
      tmp = tmp.substring(tmp.indexOf("/") + 1);
      tmp = tmp.substring(0, tmp.indexOf("/"));
      loginState = loginState + "-" + tmp;
    }

    // Check that the loginState is a valid one.
    final String finalLoginState = loginState;
    if (Arrays.stream(LoginState.values())
        .noneMatch(e -> e.getCode().equals(finalLoginState))) {
      throw new RuntimeException(
          "loginState not appropriate: loginState = " + loginState + ", urlPath = " + urlPath);
    }

    return loginState;
  }
}
