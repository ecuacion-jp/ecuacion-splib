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
package jp.ecuacion.splib.web.bean;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import jp.ecuacion.lib.core.annotation.RequireNonnull;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.util.SplibUtil;
import jp.ecuacion.splib.web.util.internal.TransactionTokenUtil;

/**
 * Returns the URL to redirect / forward pages to use as the return string in 
 * {@code @RequestMapping, @GetMapping and @PostMapping} methods.
 */
public class ReturnUrlBean {

  /*
   * Is the protocol (redirect / forward) part of the URL.
   */
  private boolean isForward = false;

  /*
   * Is the path part of the URL.
   */
  @Nonnull
  private String path;

  /**
   * Is the parameters part of the URL.
   * 
   * <p>The data type of the value is an array 
   *     because html request parameter allows multiple values.</p>
   */
  @Nonnull
  private final Map<String, String[]> paramMap = new HashMap<>();

  /**
   * Constructs a new instance with controller, util.
   * 
   * <p>Since {@code isNormalEnd == false} is used 
   *     only from ExceptionHandler normally implemented not by app-developers,
   *     {@code isNormalEnd} is set to {@code true} by default.</p>
   *  
   * @param controller controller
   * @param util util
   */
  public ReturnUrlBean(@RequireNonnull SplibGeneralController<?> controller,
      @RequireNonnull SplibUtil util) {
    this(controller, util, true);
  }

  /**
   * Constructs a new instance with controller, util, isNormalEnd.
   * 
   * @param isNormalEnd when it's {@code true} 
   *     the default subFunction and page on normal end set in the controller is used. 
   * @param controller controller
   * @param util util
   */
  public ReturnUrlBean(@RequireNonnull SplibGeneralController<?> controller,
      @RequireNonnull SplibUtil util, boolean isNormalEnd) {
    this(ObjectsUtil.requireNonNull(controller), util, isNormalEnd,
        isNormalEnd ? controller.getDefaultDestPageOnNormalEnd()
            : controller.getDefaultDestPageOnAbnormalEnd());
  }

  /**
   * Constructs a new instance with controller, util, page.
   * 
   * <p>Since {@code isNormalEnd == false} is used 
   *     only from ExceptionHandler normally implemented not by app-developers,
   *     {@code isNormalEnd} is set to {@code true} by default.</p>
   *     
   * @param controller controller
   * @param util util
   * @param page page
   */
  public ReturnUrlBean(@RequireNonnull SplibGeneralController<?> controller,
      @RequireNonnull SplibUtil util, @RequireNonnull String page) {
    this(controller, util, true, page);
  }

  /**
   * Constructs a new instance with controller, util, page.
   * 
   * @param isNormalEnd when it's {@code true} 
   *     the default subFunction on normal end set in the controller is used. 
   * @param controller controller
   * @param util util
   * @param page page
   */
  public ReturnUrlBean(@RequireNonnull SplibGeneralController<?> controller,
      @RequireNonnull SplibUtil util, boolean isNormalEnd, @RequireNonnull String page) {
    this(ObjectsUtil.requireNonNull(controller), util,
        isNormalEnd ? controller.getDefaultDestSubFunctionOnNormalEnd()
            : controller.getDefaultDestSubFunctionOnAbnormalEnd(),
        page);
  }

  /**
   * Constructs a new instance with controller, util, subFunction, page.
   * 
   * @param controller controller
   * @param util util
   * @param subFunction subFunction
   * @param page page
   */
  public ReturnUrlBean(@RequireNonnull SplibGeneralController<?> controller,
      @RequireNonnull SplibUtil util, @RequireNonnull String subFunction,
      @RequireNonnull String page) {
    path = getPathFromParams(ObjectsUtil.requireNonNull(controller),
        ObjectsUtil.requireNonNull(util).getLoginState(),
        ObjectsUtil.requireNonNull(subFunction), ObjectsUtil.requireNonNull(page));

    putParamList(controller.getParamListOnRedirectToSelf());
  }

  /**
   * Constructs a new instance with path.
   * 
   * @param controller controller
   * @param path path
   */
  public ReturnUrlBean(@RequireNonnull SplibGeneralController<?> controller,
      @RequireNonnull String path) {
    this.path = ObjectsUtil.requireNonNull(path);

    putParamList(controller.getParamListOnRedirectToSelf());
  }

  /**
   * Returns the URL.
   * 
   * @return URL string
   */
  public String getUrl() {
    return getProtocol() + ":" + path + getParamsString();
  }

  public boolean isForward() {
    return isForward;
  }

  /**
   * Sets isForward.
   * 
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean setProtocolForward() {
    this.isForward = true;
    putParam("forwarded", (String) null);
    return this;
  }

  /**
   * Returns protocol string.
   * 
   * @return protocol
   */
  private String getProtocol() {
    return isForward ? "forward" : "redirect";
  }

  /**
   * Returns path by concatenating the arguments.
   * 
   * @param ctrl ctrl
   * @param loginState loginState
   * @param subFunction subFunction
   * @param page page
   * @return path
   */
  private String getPathFromParams(SplibGeneralController<?> ctrl, String loginState,
      String subFunction, String page) {

    String subFuncPart = (subFunction.equals("") ? "" : "/" + subFunction);

    return "/" + loginState + "/" + ctrl.getFunction() + subFuncPart + "/" + page;
  }

  /**
   * Constructs the parameter part of the URL from {@code paramMap} and returns it.
   *  
   * @return parameter part of the URL
   */
  private String getParamsString() {
    // requestから来たparameterをまとめてRedirectUrlBeanに追加する場合、
    // transactionTokenが入ってきてしまうがそのままredirectするとチェックエラーになるのでtransactionTokenをparamsから除く処理を追加
    removeParam(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN);

    // forwardの場合はそれとわかるparamterを追加
    if (isForward) {
      putParam("forward", "true");
    }

    boolean is1st = true;
    StringBuilder sb = new StringBuilder();
    // forwardのparamを追加
    for (Entry<String, String[]> entry : paramMap.entrySet()) {
      for (String value : entry.getValue()) {
        if (is1st) {
          is1st = false;
          sb.append("?");

        } else {
          sb.append("&");
        }

        sb.append(entry.getKey() + "=" + value);
      }
    }

    return sb.toString();
  }

  /**
   * Adds the argument parameter to {@code paramMap}.
   * 
   * @param key key
   * @param value value
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean putParam(String key, String value) {
    Objects.requireNonNull(key);
    paramMap.put(key, value == null ? new String[] {""} : new String[] {value});

    return this;
  }

  /**
   * Adds the argument parameter to {@code paramMap}.
   * 
   * @param key key
   * @param values values
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean putParam(String key, String[] values) {
    Objects.requireNonNull(key);
    paramMap.put(key, values == null ? new String[] {""} : values);

    return this;
  }

  /**
   * Adds the argument map to {@code paramMap}.
   * 
   * <p>This is used when you want to put {@code request.parameterMap()} to the paramMap.</p>
   * 
   * @param paramMap paramMap
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean putParamMap(Map<String, String[]> paramMap) {
    Objects.requireNonNull(paramMap);
    this.paramMap.putAll(paramMap);

    return this;
  }

  private void putParamList(List<String[]> keyValueList) {
    for (String[] keyValue : keyValueList) {
      putParam(keyValue[0], keyValue[1]);
    }
  }


  /**
   * Removes the argument key from {@code paramMap}.
   * 
   * @param key key
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean removeParam(String key) {
    paramMap.remove(key);
    return this;
  }

  /**
   * add parameter to show success message.
   * 
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean showSuccessMessage() {
    putParam("success", new String[] {""});
    return this;
  }
}
