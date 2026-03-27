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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;
import jp.ecuacion.splib.web.bean.ReturnUrlBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Provides utility methods for splib-web.
 */
@Component
public class SplibUtil {

  @Autowired
  HttpServletRequest request;

  @Autowired
  private ServletContext servletContext;

  /**
   * Returns DatetimeFormatParameters.
   */
  public DatetimeFormatParameters getParams(HttpServletRequest request) {
    DatetimeFormatParameters params = new DatetimeFormatParameters();
    String strOffset = (String) request.getSession().getAttribute("zoneOffset");

    // strOffset can be null due to irregular behavior,
    // such as when a system error occurs and the user is redirected to the login screen
    // even though userDetails are still in the session.
    // In that case the subsequent processing would fail with a null argument,
    // so set it to UTC (0) when null.
    // This should be fine since time is unlikely to be displayed on non-login screens.
    if (strOffset == null) {
      strOffset = "0";
    }

    params.setZoneOffsetWithJsMinutes(Integer.parseInt(strOffset));

    return params;
  }

  /* ----------- */
  /* url related */
  /* ----------- */

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
    if ((!contextPath.equals("") && !contextPath.startsWith("/"))
        || !urlPathWithContextPath.startsWith("/")) {
      throw new RuntimeException(
          "servletContext.getContextPath() or request.getRequestURI() does not start with"
              + " \"/\". Unexpected. servletContext.getContextPath() = " + contextPath
              + ", request.getRequestURI() = " + urlPathWithContextPath);
    }

    String urlPath = urlPathWithContextPath;

    // In addition to contextPath == "", also handle contextPath == "/"; ignore both cases.
    if (!contextPath.equals("") && !contextPath.equals("/")) {
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
    List<String> loginStateCodeList = new ArrayList<>();
    for (LoginStateEnum anEnum : LoginStateEnum.values()) {
      loginStateCodeList.add(anEnum.getCode());
    }

    if (!loginStateCodeList.contains(loginState)) {
      throw new RuntimeException(
          "loginState not appropriate: loginState = " + loginState + ", urlPath = " + urlPath);
    }

    return loginState;
  }

  /**
   * Prepares for page transition and returns url.
   * 
   * @param request request
   * @param redirectBean redirectBean
   * @param model model
   * @param takeOverMessages takeOverMessages
   * @return String
   */
  public String prepareForPageTransition(HttpServletRequest request,
      ReturnUrlBean redirectBean, Model model, boolean takeOverMessages) {
    
    // contextId and contextMap
    String contextId = UUID.randomUUID().toString();
    Map<String, Object> contextMap = new HashMap<>();
    request.getSession().setAttribute(SplibWebConstants.KEY_CONTEXT_MAP_PREFIX + contextId,
        contextMap);

    // messagesBean
    if (takeOverMessages) {
      contextMap.put(SplibWebConstants.KEY_MESSAGES_BEAN,
          model.getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));
    }

    // model
    if (model != null) {
      // For same-page transitions, make the model available at the redirect destination.
      contextMap.put(SplibWebConstants.KEY_MODEL, model);
    }

    // Add the current UUID to redirectBean.
    redirectBean.putParam(SplibWebConstants.KEY_CONTEXT_ID, new String[] {contextId.toString()});

    // returns url
    return redirectBean.getUrl();
  }

  /**
   * Designates login state.
   * 
   * <p>public=no login, account=user login, admin=service provider or administrator login.</p>
   * 
   * <p>It's used the following.</p>
   * <ul>
   * <li>As a part of url: xxx of {@code https://domain/contextPath/xxx/....}.
   * <li>Specifies the kind of navBar. Totally different navBar can be shown by each loginState.
   * </ul>
   */
  public static enum LoginStateEnum {

    PUBLIC("public"),

    ACCOUNT("account"),

    ADMIN("admin"),

    /**
     * It's the path for common for ecuacion libraries like config.
     * 
     * <p>As url it becumes {@code ecuacion/public}.
     */
    ECUACION_PUBLIC("ecuacion-public");

    private String code;

    private LoginStateEnum(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }
  }
}
