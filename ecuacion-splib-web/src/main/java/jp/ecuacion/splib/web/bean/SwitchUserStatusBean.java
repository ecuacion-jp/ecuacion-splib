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

import jp.ecuacion.splib.web.config.SplibWebSecurityConfigForSwitchUser;
import jp.ecuacion.splib.webmvc.util.SwitchUserUtil;

/**
 * Provides the state of the switch user to Thymeleaf templates.
 *
 * <p>Registered by {@link SplibWebSecurityConfigForSwitchUser} under the name
 *     {@value SplibWebSecurityConfigForSwitchUser#BEAN_NAME_SWITCH_USER_STATUS}, so that templates
 *     can hide what cannot be used while switched, for example:</p>
 *
 * <pre>{@code
 * <button th:unless="${@splibSwitchUserStatus.readOnly}" ...>Save</button>
 * }</pre>
 *
 * <p>Both look at the current {@code Authentication} with {@link SwitchUserUtil}.</p>
 */
public class SwitchUserStatusBean {

  private final boolean readOnlyWhileSwitched;

  /**
   * Constructs a new instance with {@code readOnlyWhileSwitched}.
   *
   * @param readOnlyWhileSwitched whether requests that change something are rejected while
   *     switched
   */
  public SwitchUserStatusBean(boolean readOnlyWhileSwitched) {
    this.readOnlyWhileSwitched = readOnlyWhileSwitched;
  }

  /**
   * Returns whether the current user is a switched user.
   *
   * @return {@code true} if switched
   */
  public boolean isSwitchedUser() {
    return SwitchUserUtil.isSwitchedUser();
  }

  /**
   * Returns whether the current user can only view pages: switched, and requests that change
   *     something are rejected while switched.
   *
   * @return {@code true} if the current user can only view pages
   */
  public boolean isReadOnly() {
    return readOnlyWhileSwitched && isSwitchedUser();
  }
}
