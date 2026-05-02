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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.internal.TransactionTokenUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
  private String path = "";

  /**
   * Is the parameters part of the URL.
   * 
   * <p>The data type of the value is an array 
   *     because html request parameter allows multiple values.</p>
   */
  private final Map<String, String[]> paramMap = new HashMap<>();

  /**
   * Constructs a new instance with controller, loginStateUtil.
   *
   * <p>Since {@code isNormalEnd == false} is used
   *     only from ExceptionHandler normally implemented not by app-developers,
   *     {@code isNormalEnd} is set to {@code true} by default.</p>
   *
   * @param controller controller
   * @param loginStateUtil loginStateUtil
   */
  public ReturnUrlBean(SplibGeneralController<?> controller, SplibLoginStateUtil loginStateUtil) {
    this(controller, loginStateUtil, true);
  }

  /**
   * Constructs a new instance with controller, loginStateUtil, isNormalEnd.
   *
   * @param isNormalEnd when it's {@code true}
   *     the default subFunction and page on normal end set in the controller is used.
   * @param controller controller
   * @param loginStateUtil loginStateUtil
   */
  public ReturnUrlBean(SplibGeneralController<?> controller, SplibLoginStateUtil loginStateUtil,
      boolean isNormalEnd) {
    this(ObjectsUtil.requireNonNull(controller), loginStateUtil, isNormalEnd,
        isNormalEnd ? controller.getDefaultDestPageOnNormalEnd()
            : controller.getDefaultDestPageOnAbnormalEnd());
  }

  /**
   * Constructs a new instance with controller, loginStateUtil, page.
   *
   * <p>Since {@code isNormalEnd == false} is used
   *     only from ExceptionHandler normally implemented not by app-developers,
   *     {@code isNormalEnd} is set to {@code true} by default.</p>
   *
   * @param controller controller
   * @param loginStateUtil loginStateUtil
   * @param page page
   */
  public ReturnUrlBean(SplibGeneralController<?> controller, SplibLoginStateUtil loginStateUtil,
      String page) {
    this(controller, loginStateUtil, true, page);
  }

  /**
   * Constructs a new instance with controller, loginStateUtil, page.
   *
   * @param isNormalEnd when it's {@code true}
   *     the default subFunction on normal end set in the controller is used.
   * @param controller controller
   * @param loginStateUtil loginStateUtil
   * @param page page
   */
  public ReturnUrlBean(SplibGeneralController<?> controller, SplibLoginStateUtil loginStateUtil,
      boolean isNormalEnd, String page) {
    this(ObjectsUtil.requireNonNull(controller), loginStateUtil,
        isNormalEnd ? controller.getDefaultDestSubFunctionOnNormalEnd()
            : controller.getDefaultDestSubFunctionOnAbnormalEnd(),
        page);
  }

  /**
   * Constructs a new instance with controller, loginStateUtil, subFunction, page.
   *
   * @param controller controller
   * @param loginStateUtil loginStateUtil
   * @param subFunction subFunction
   * @param page page
   */
  public ReturnUrlBean(SplibGeneralController<?> controller, SplibLoginStateUtil loginStateUtil,
      String subFunction, String page) {
    path = getPathFromParams(ObjectsUtil.requireNonNull(controller),
        ObjectsUtil.requireNonNull(loginStateUtil).getLoginState(),
        ObjectsUtil.requireNonNull(subFunction), ObjectsUtil.requireNonNull(page));

    putParamList(controller.getParamListOnRedirectToSelf());
  }

  /**
   * Constructs a new instance with explicit path.
   * 
   * @param path path
   */
  public ReturnUrlBean(String path) {
    this.path = ObjectsUtil.requireNonNull(path);
  }

  /**
   * Returns the URL.
   *
   * @return URL string
   */
  public String getUrl() {
    return getProtocol() + ":" + path + getParamsString();
  }

  /**
   * Prepares for page transition by carrying over the model via flash attributes,
   * and returns the URL.
   *
   * <p>When {@code takeOverMessages} is {@code false}, warning / success-flag /
   *     {@code BindingResult}-prefixed entries are stripped from the snapshot so the
   *     redirected page starts with a clean error/warning state.</p>
   *
   * @param model model
   * @param redirectAttributes redirectAttributes
   * @param takeOverMessages whether to carry over warning / error / success messages
   * @return URL string
   */
  public String applyTo(Model model, RedirectAttributes redirectAttributes,
      boolean takeOverMessages) {

    Map<String, Object> modelSnapshot = new HashMap<>(model.asMap());

    if (!takeOverMessages) {
      modelSnapshot.remove(SplibWebConstants.KEY_WARN_MESSAGE);
      modelSnapshot.remove(SplibWebConstants.KEY_NEEDS_SUCCESS_MESSAGE);
      modelSnapshot.entrySet()
          .removeIf(entry -> entry.getKey().startsWith(BindingResult.MODEL_KEY_PREFIX));
    }

    redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_SAVED_MODEL, modelSnapshot);

    return getUrl();
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
    String functionKindPath = ctrl.getFunctionKinds().length == 0 ? ""
        : (StringUtil.getSeparatedValuesString(ctrl.getFunctionKinds(), "/") + "/");
    String subFuncPart = (subFunction.isEmpty() ? "" : "/" + subFunction);

    return "/" + loginState + "/" + functionKindPath + ctrl.getFunction() + subFuncPart + "/"
        + page;
  }

  /**
   * Constructs the parameter part of the URL from {@code paramMap} and returns it.
   *  
   * @return parameter part of the URL
   */
  private String getParamsString() {
    // When adding request parameters to RedirectUrlBean in bulk, the transactionToken may be
    // included, but redirecting with it would cause a check error,
    // so remove transactionToken from params.
    removeParam(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN);

    // If forwarding, add a parameter to indicate that.
    if (isForward) {
      putParam("forward", "true");
    }

    boolean is1st = true;
    StringBuilder sb = new StringBuilder();
    // Add forward param to URL.
    for (Entry<String, String[]> entry : paramMap.entrySet()) {
      for (String value : entry.getValue()) {
        if (is1st) {
          is1st = false;
          sb.append(path.contains("?") ? "&" : "?");

        } else {
          sb.append("&");
        }

        sb.append(entry.getKey()).append("=").append(value);
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
  public ReturnUrlBean putParam(String key, @Nullable String value) {
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
  public ReturnUrlBean putParam(String key, @Nullable String[] values) {
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
