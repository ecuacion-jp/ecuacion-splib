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
/// *
// * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
// *
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
/// except
// * in compliance with the License. You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software distributed under the
/// License
// * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
/// express
// * or implied. See the License for the specific language governing permissions and limitations
/// under
// * the License.
// */
// package jp.ecuacion.splib.web.bean;
//
// import jp.ecuacion.splib.web.controller.SplibGeneralController;
// import jp.ecuacion.splib.web.util.SplibUtil;
//
// public abstract class RedirectUrlPageBean extends RedirectUrlBean {
//
// public RedirectUrlPageBean(SplibGeneralController<?> ctrl, SplibUtil util) {
// this(ctrl, util, null);
// }
//
// public RedirectUrlPageBean(SplibGeneralController<?> ctrl, SplibUtil util, String page) {
// this(ctrl, util, null, page);
// }
//
// public RedirectUrlPageBean(SplibGeneralController<?> ctrl, SplibUtil util, String subFunction,
// String page) {
// super();
//
// path = getPathFromParams(ctrl, util.getLoginState(), subFunction, page);
// }
//
// public RedirectUrlPageBean(String path) {
// this.path = path;
// }
//
// private String getPathFromParams(SplibGeneralController<?> ctrl, String loginState,
// String subFunction, String page) {
// String defaultSubFunction = getDefaultRedirectDestSubFunction(ctrl);
//
// String subFuncPart = (subFunction == null || subFunction.equals("")
// ? (defaultSubFunction == null || defaultSubFunction.equals("") ? ""
// : "/" + defaultSubFunction)
// : "/" + subFunction);
//
// return "/" + loginState + "/" + ctrl.getFunction() + subFuncPart + "/"
// + (page == null || page.equals("") ? getDefaultRedirectDestPage(ctrl) : page);
// }
//
// protected abstract String getDefaultRedirectDestSubFunction(SplibGeneralController<?> ctrl);
//
// protected abstract String getDefaultRedirectDestPage(SplibGeneralController<?> ctrl);
// }


