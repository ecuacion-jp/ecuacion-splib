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
package jp.ecuacion.splib.web.constant;

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
public enum LoginState {

  /** No login required. */
  PUBLIC("public"),

  /** End-user login. */
  ACCOUNT("account"),

  /** Service provider or administrator login. */
  ADMIN("admin"),

  /**
   * It's the path for common for ecuacion libraries like config.
   *
   * <p>As url it becumes {@code ecuacion/public}.
   */
  ECUACION_PUBLIC("ecuacion-public");

  private String code;

  private LoginState(String code) {
    this.code = code;
  }

  /**
   * Returns the code of the login state.
   *
   * @return code
   */
  public String getCode() {
    return code;
  }
}
