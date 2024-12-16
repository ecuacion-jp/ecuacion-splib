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

public class RedirectUrlPageBean extends RedirectUrlBean {

  private String subFunction;
  private String page;

  public RedirectUrlPageBean() {
    this(null);
  }

  public RedirectUrlPageBean(String page) {
    this(null, page);
  }

  public RedirectUrlPageBean(String subFunction, String page) {
    super();
    this.subFunction = subFunction;
    this.page = page;
  }

  public String getUrl(String loginState, String function,
      String defaultSubFunction, String defaultPage) {
    String subFuncPart = (subFunction == null || subFunction.equals("")
        ? (defaultSubFunction == null || defaultSubFunction.equals("") ? ""
            : "/" + defaultSubFunction)
        : "/" + subFunction);
    String urlPath = getProtocol() + ":/" + loginState + "/" + function + subFuncPart
        + "/" + (page == null || page.equals("") ? defaultPage : page) + getParamsString();
    return urlPath;
  }
}
