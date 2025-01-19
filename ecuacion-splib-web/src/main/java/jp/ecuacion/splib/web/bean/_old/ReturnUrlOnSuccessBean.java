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
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
// package jp.ecuacion.splib.web.bean;
//
// import jp.ecuacion.splib.web.controller.SplibGeneralController;
// import jp.ecuacion.splib.web.util.SplibUtil;
//
/// **
// * Returns the URL to redirect / forward pages to use as the return string in
// * {@code @RequestMapping, @GetMapping and @PostMapping} methods on the successful procedure.
// */
// public class ReturnUrlOnSuccessBean extends ReturnUrlBean {
//
// /**
// * Constructs a new instance with controller, util.
// *
// * @param controller controller
// * @param util util
// */
// public ReturnUrlOnSuccessBean(SplibGeneralController<?> controller, SplibUtil util) {
// super(controller, util);
// putParam("success", new String[] {""});
// }
//
// /**
// * Constructs a new instance with controller, util, page.
// *
// * @param controller controller
// * @param util util
// * @param page page
// */
// public ReturnUrlOnSuccessBean(SplibGeneralController<?> controller, SplibUtil util, String page)
/// {
// super(controller, util, page);
// putParam("success", new String[] {""});
// }
//
// /**
// * Constructs a new instance with controller, util, subFunction, page.
// *
// * @param controller controller
// * @param util util
// * @param subFunction subFunction
// * @param page page
// */
// public ReturnUrlOnSuccessBean(SplibGeneralController<?> controller, SplibUtil util,
// String subFunction, String page) {
// super(controller, util, subFunction, page);
// putParam("success", new String[] {""});
// }
//
// /**
// * Constructs a new instance with path.
// *
// * @param path path
// */
// public ReturnUrlOnSuccessBean(String path) {
// super(path);
// putParam("success", new String[] {""});
// }
//
// /**
// * Removes {@code success} parameter.
// *
// * @return ReturnUrlBean (for method chain)
// */
// public ReturnUrlOnSuccessBean noSuccessMessage() {
// removeParam("success");
// return this;
// }
//
// @Override
// protected String getDefaultRedirectDestSubFunction(SplibGeneralController<?> ctrl) {
// return ctrl.getDefaultRedirectDestSubFunctionOnSuccess();
// }
//
// @Override
// protected String getDefaultRedirectDestPage(SplibGeneralController<?> ctrl) {
// return ctrl.getDefaultRedirectDestPageOnSuccess();
// }
// }


