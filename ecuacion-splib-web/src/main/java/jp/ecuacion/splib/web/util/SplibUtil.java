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

    // イレギュラーな挙動により、strOffsetがnullの場合は発生してしまう。
    // （システムエラーで、userDetailはsessionに持っているのにlogin画面に戻る場合など）
    // その場合、次の処理がnull引数でエラーとなるためnullならUtc（0）を入れておく。
    // ログインしていない画面で時間を表示することもないだろうから問題ないと思われる
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

    // 動作確認した限りは、servletContext.getContextPath()（以下A）は、"/contextPath"のように前のみに"/"がつく、
    // request.getRequestURI()（以下B）は、"/contextPath/public/..." のように記載、なので、
    // B.replaceFirst(A)は、頭のcontextPath部分が綺麗にとれて"/public/..."のようにcontextPathがない場合と同一になる想定だが、
    // 実はservletContext.getContextPath()で頭に"/"がない形で取得されるパターンがあった、などのイレギュラーがあると嫌なので念の為チェックしておく
    if ((!contextPath.equals("") && !contextPath.startsWith("/"))
        || !urlPathWithContextPath.startsWith("/")) {
      throw new RuntimeException(
          "servletContext.getContextPath() または request.getRequestURI() が\"\\\"から始まっていません。想定外です。"
              + "servletContext.getContextPath() = " + contextPath + ", request.getRequestURI() = "
              + urlPathWithContextPath);
    }

    String urlPath = urlPathWithContextPath;

    // contextPath == "" の場合に加え、contextPath == "/" の場合も考慮。これらの場合は無視する
    if (!contextPath.equals("") && !contextPath.equals("/")) {
      urlPath = urlPath.replaceFirst(servletContext.getContextPath(), "");
    }

    String tmp = urlPath.substring(1);
    String loginState = tmp.substring(0, tmp.indexOf("/"));

    // loginStateが"ecuacion"だった場合は、次の階層の文字列も連結してloginStateとする
    // 例：/ecuacion/public/... -> "ecuacion-public"
    if (loginState.equals("ecuacion")) {
      // この時点のtmpは、頭の/がとれたecuacion/public/... の状態。そこから"public"を取り出す
      tmp = tmp.substring(tmp.indexOf("/") + 1);
      tmp = tmp.substring(0, tmp.indexOf("/"));
      loginState = loginState + "-" + tmp;
    }

    // 存在するloginStateかチェック
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
      // 自画面遷移の場合はmodelもredirect先で取得可にしておく
      contextMap.put(SplibWebConstants.KEY_MODEL, model);
    }

    // redirectBeanに今のuuidを追加設定しておく。
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
