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

import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.util.SplibUtil;

public class RedirectUrlPageOnSuccessBean extends RedirectUrlPageBean
    implements RedirectUrlOnSuccessInterface {

  // public RedirectUrlPageOnSuccessBean() {
  // super();
  // putParam("success", new String[] {""});
  // }
  //
  // public RedirectUrlPageOnSuccessBean(String page) {
  // super(page);
  // putParam("success", new String[] {""});
  // }
  //
  // public RedirectUrlPageOnSuccessBean(String subFunction, String page) {
  // super(subFunction, page);
  // putParam("success", new String[] {""});
  // }

  public RedirectUrlPageOnSuccessBean(SplibGeneralController<?> ctrl, SplibUtil util) {
    super(ctrl, util);
    putParam("success", new String[] {""});
  }

  public RedirectUrlPageOnSuccessBean(SplibGeneralController<?> ctrl, SplibUtil util, String page) {
    super(ctrl, util, page);
    putParam("success", new String[] {""});
  }

  public RedirectUrlPageOnSuccessBean(SplibGeneralController<?> ctrl, SplibUtil util, String subFunction, String page) {
    super(ctrl, util, subFunction, page);
    putParam("success", new String[] {""});
  }

  /** method chain形式にしておく。 */
  public RedirectUrlPageOnSuccessBean noSuccessMessage() {
    removeParam("success");
    return this;
  }
}
