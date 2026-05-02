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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.internal.TransactionTokenUtil;
import org.jspecify.annotations.Nullable;

/**
 * Returns the URL to redirect / forward pages to use as the return string in
 * {@code @RequestMapping, @GetMapping and @PostMapping} methods.
 *
 * <p>Construction is via static factory methods:</p>
 * <ul>
 *   <li>{@link #forNormalEnd(SplibGeneralController, SplibLoginStateUtil)} —
 *       used by application controllers on normal end.</li>
 *   <li>{@link #forAbnormalEnd(SplibGeneralController, SplibLoginStateUtil)} —
 *       used internally by exception handlers; not for direct use by application code.</li>
 *   <li>{@link #ofPath(String)} — wraps an explicit path string.</li>
 * </ul>
 *
 * <p>Optional adjustments are applied via setter chain methods such as
 * {@link #toSubFunction(String)}, {@link #toPage(String)},
 * {@link #putParam(String, String)}, {@link #showSuccessMessage()}, and
 * {@link #asForward()}. Call {@link #getUrl()} to obtain the final URL string.</p>
 *
 * <p>Saving the model into flash attributes for the redirect target is handled by
 * {@code SplibSavedModelUtil#saveToFlash}, not by this class.</p>
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
   *
   * <p>{@code LinkedHashMap} is used so that the parameter order in the produced URL
   *     follows insertion order and is therefore deterministic.</p>
   */
  private final Map<String, String[]> paramMap = new LinkedHashMap<>();

  /**
   * Holds the controller used to construct the path.
   *
   * <p>{@code null} when this instance was created via {@link #ofPath(String)}.</p>
   */
  private final @Nullable SplibGeneralController<?> controller;

  /**
   * Holds the login state used to construct the path.
   *
   * <p>{@code null} when this instance was created via {@link #ofPath(String)}.</p>
   */
  private final @Nullable String loginState;

  /**
   * Holds the current subFunction used to construct the path.
   */
  private String subFunction = "";

  /**
   * Holds the current page used to construct the path.
   */
  private String page = "";

  /**
   * Constructs a new instance from controller-derived path components.
   */
  private ReturnUrlBean(SplibGeneralController<?> controller, SplibLoginStateUtil loginStateUtil,
      String subFunction, String page) {
    this.controller = Objects.requireNonNull(controller);
    this.loginState = Objects.requireNonNull(loginStateUtil).getLoginState();
    this.subFunction = Objects.requireNonNull(subFunction);
    this.page = Objects.requireNonNull(page);
    this.path = buildPath(controller, Objects.requireNonNull(loginState), subFunction, page);

    putParamList(controller.getParamListOnRedirectToSelf());
  }

  /**
   * Constructs a new instance from an explicit path.
   */
  private ReturnUrlBean(String path) {
    this.controller = null;
    this.loginState = null;
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Creates an instance for a normal-end redirect.
   *
   * <p>The default subFunction and page configured on the controller for normal end are used.
   *     Override them with {@link #toSubFunction(String)} / {@link #toPage(String)} as needed.</p>
   *
   * @param controller controller
   * @param loginStateUtil loginStateUtil
   * @return ReturnUrlBean
   */
  public static ReturnUrlBean forNormalEnd(SplibGeneralController<?> controller,
      SplibLoginStateUtil loginStateUtil) {
    Objects.requireNonNull(controller);
    return new ReturnUrlBean(controller, loginStateUtil,
        controller.getDefaultDestSubFunctionOnNormalEnd(),
        controller.getDefaultDestPageOnNormalEnd());
  }

  /**
   * Creates an instance for an abnormal-end redirect.
   *
   * <p>This factory is intended to be used from exception handling code paths
   *     ({@code SplibExceptionHandler} and library controllers preparing
   *     {@code redirectUrlOnAppExceptionBean}). Application code does not normally call this.</p>
   *
   * @param controller controller
   * @param loginStateUtil loginStateUtil
   * @return ReturnUrlBean
   */
  public static ReturnUrlBean forAbnormalEnd(SplibGeneralController<?> controller,
      SplibLoginStateUtil loginStateUtil) {
    Objects.requireNonNull(controller);
    return new ReturnUrlBean(controller, loginStateUtil,
        controller.getDefaultDestSubFunctionOnAbnormalEnd(),
        controller.getDefaultDestPageOnAbnormalEnd());
  }

  /**
   * Creates an instance from an explicit path.
   *
   * <p>{@link #toSubFunction(String)} and {@link #toPage(String)} are not applicable
   *     for instances created this way and will throw {@code IllegalStateException}.</p>
   *
   * @param path path
   * @return ReturnUrlBean
   */
  public static ReturnUrlBean ofPath(String path) {
    return new ReturnUrlBean(Objects.requireNonNull(path));
  }

  /**
   * Overrides the subFunction part of the path.
   *
   * @param subFunction subFunction
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean toSubFunction(String subFunction) {
    Objects.requireNonNull(subFunction);
    this.subFunction = subFunction;
    rebuildPathFromController("toSubFunction");
    return this;
  }

  /**
   * Overrides the page part of the path.
   *
   * @param page page
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean toPage(String page) {
    Objects.requireNonNull(page);
    this.page = page;
    rebuildPathFromController("toPage");
    return this;
  }

  private void rebuildPathFromController(String methodName) {
    SplibGeneralController<?> ctrl = controller;
    String state = loginState;
    if (ctrl == null || state == null) {
      throw new IllegalStateException(
          methodName + "() is not applicable for instances created via ofPath().");
    }
    this.path = buildPath(ctrl, state, subFunction, page);
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
   * Switches the URL protocol from {@code redirect} to {@code forward}.
   *
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean asForward() {
    this.isForward = true;
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
   * Builds the path by concatenating the arguments.
   */
  private static String buildPath(SplibGeneralController<?> ctrl, String loginState,
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
   * <p>This method does not mutate {@code paramMap}; it works on a local copy
   *     so that calling {@link #getUrl()} multiple times yields the same result
   *     and leaves the bean's state untouched.</p>
   *
   * <p>Keys and values are URL-encoded with UTF-8.</p>
   *
   * @return parameter part of the URL
   */
  private String getParamsString() {
    Map<String, String[]> effectiveParams = new LinkedHashMap<>(paramMap);

    // When adding request parameters to RedirectUrlBean in bulk, the transactionToken may be
    // included, but redirecting with it would cause a check error,
    // so remove transactionToken from params.
    effectiveParams.remove(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN);

    // If forwarding, add a parameter to indicate that
    // (consumed by SplibGeneralController#transactionTokenCheck to skip the check).
    if (isForward) {
      effectiveParams.put("forward", new String[] {"true"});
    }

    boolean is1st = true;
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String[]> entry : effectiveParams.entrySet()) {
      for (String value : entry.getValue()) {
        if (is1st) {
          is1st = false;
          sb.append(path.contains("?") ? "&" : "?");

        } else {
          sb.append("&");
        }

        sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
            .append("=")
            .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
      }
    }

    return sb.toString();
  }

  /**
   * Adds the argument parameter to {@code paramMap}.
   *
   * <p>A {@code null} {@code value} is treated as an empty string and produces
   *     {@code key=} in the URL — the same output as passing {@code ""}. To omit
   *     the parameter entirely, do not call this method (or call
   *     {@link #removeParam(String)}).</p>
   *
   * @param key key
   * @param value value; {@code null} is normalized to empty string
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
   * <p>A {@code null} {@code values} array is treated as a single empty-string value
   *     and produces {@code key=} in the URL.</p>
   *
   * @param key key
   * @param values values; {@code null} is normalized to a single empty-string value
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean putParam(String key, @Nullable String[] values) {
    Objects.requireNonNull(key);
    paramMap.put(key, values == null ? new String[] {""} : values.clone());

    return this;
  }

  /**
   * Adds the argument map to {@code paramMap}.
   *
   * <p>This is used when you want to put {@code request.parameterMap()} to the paramMap.</p>
   *
   * <p>The value arrays are defensively copied, so later mutations to the source
   *     map's arrays do not affect this bean's state.</p>
   *
   * @param paramMap paramMap
   * @return ReturnUrlBean (for method chain)
   */
  public ReturnUrlBean putParamMap(Map<String, String[]> paramMap) {
    Objects.requireNonNull(paramMap);
    for (Entry<String, String[]> entry : paramMap.entrySet()) {
      this.paramMap.put(entry.getKey(), entry.getValue().clone());
    }

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
