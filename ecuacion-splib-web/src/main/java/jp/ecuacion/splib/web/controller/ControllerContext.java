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
package jp.ecuacion.splib.web.controller;

import java.util.Objects;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import org.jspecify.annotations.Nullable;

/**
 * Stores controller properties.
 *
 * <p>Since properties are many,
 *     the properties are passed not by the constructor, but by method chains.</p>
 */
public class ControllerContext {

  /**
   * Constructs a new instance.
   *
   * <p>It's not public because it's supposed
   * to use {@code newContext()} to get the new instance.</p>
   */
  ControllerContext() {

  }


  /**
   * See functionKind().
   */
  private String[] functionKinds = new String[] {};

  /**
   * See function().
   */
  private String function = "";

  /**
   * See subFunction().
   */
  private String subFunction = "";

  /**
   * See htmlFilenamePostfix().
   */
  @Nullable
  private String htmlFilenamePostfix;

  /**
   * See rootRecordName().
   */
  @Nullable
  private String mainRootRecordName;

  /**
   * Stores {@code functionKind} and returns {@code ControllerContext}.
   *
   * @param functionKinds functionKinds
   */
  public ControllerContext functionKinds(String... functionKinds) {
    this.functionKinds = functionKinds;
    return this;
  }

  /**
   * Returns {@code functionKinds}.
   *
   * <p>{@code functionKind} designates a kind of functions. <br>
   *     It's used for paths of html files.
   *     If the default url path is {@code /public/account/page}
   *     and functionKind is {@code master-maintenance},
   *     the resultant URL would be {@code  /public/master-maintenance/account/page}.</p>
   *
   * <p>You can specify multiple clasification strings
   *     like {@code "master-maintenance", "account-related-master"}.</p>
   *
   * @return functionKinds functionKinds
   */
  public String[] functionKinds() {
    return functionKinds;
  }

  /**
   * Stores {@code function} and returns {@code ControllerContext}.
   *
   * <p>See {@link ControllerContext#function()}.</p>
   *
   * @param function function
   * @return ControllerContext
   */
  public ControllerContext function(String function) {
    this.function = function == null ? "" : function;
    return this;
  }

  /**
   * Returns a name that describes its functionality.
   *
   * <p>It is used for the prefix of various classes such as form and record,
   *     and the "xxx" part of URLs such as /account/xxx/search. <br>
   *     It is not possible to have the same function name for multiple functions.</p>
   *
   * <p>It is recommended to use the same name as the entity name, such as "accGroup".
   * (Due to implementation, the entity name is given to the function name and each class,
   * which makes it easier to understand)<br>
   * However, in reality, there can be multiple screens in which the same entity is mainly used.
   * In that case you can change the name, like "accGroupManagement" and "accGroupReference".</p>
   *
   * <p>It is not possible to specify the same function in different Controllers.
   *     The value of this field is also passed to the html side
   *     and used with the same parameter name.</p>
   *
   * @return function
   */
  public String function() {
    return function;
  }

  /**
   * Stores {@code subFunction} and returns {@code ControllerContext}.
   *
   * <p>See {@link ControllerContext#subFunction()}.</p>
   *
   * @param subFunction subFunction
   * @return ControllerContext
   */
  public ControllerContext subFunction(String subFunction) {
    this.subFunction = ObjectsUtil.requireNonNull(subFunction);
    return this;
  }

  /**
   * Returns a name that classifies the functions specified in {@code function} field.
   *
   * <p>It is used when a function includes a series of functions
   *     such as search-list-edit, the name for each function (edit).</p>
   *
   * <p>It cannot be {@code null}. Even if not specified, it's "" by default.<br>
   *
   * <p>There is usually no problem in not specifying it
   *     unless there are multiple controllers with the same function.</p>
   *
   * @return subFunction
   */
  public String subFunction() {
    return subFunction;
  }

  /**
   * Stores {@code htmlFilenamePostfix} and returns {@code ControllerContext}.
   *
   * <p>See {@link ControllerContext#htmlFilenamePostfix()}.</p>
   *
   * @param htmlFilenamePostfix htmlFilenamePostfix
   * @return ControllerContext
   */
  public ControllerContext htmlFilenamePostfix(String htmlFilenamePostfix) {
    this.htmlFilenamePostfix = htmlFilenamePostfix;
    return this;
  }

  /**
   * Returns a html filename linked to the controller.
   *
   * <p>Normally, an html filename is specified as {@code function + capitalize(subFunction)}.
   *     But if there are multiple controllers on one screen (= means "for one html file"),
   *     the subFunction of each controller and the postfix of the html file name
   *     may be different.<br>
   *     In that case, specify postfix with {@code htmlFilePostfix.}<br>
   *     The filename would be {@code function + capitalize(htmlFilenamePostfix)}.</p>
   *
   * <p>It may be {@code null}.
   *     In that case {@link SplibGeneralController#getDefaultHtmlPageName()}
   *     takes care.</p>
   *
   * @return htmlFilenamePostfix
   */
  @Nullable
  public String htmlFilenamePostfix() {
    return htmlFilenamePostfix;
  }

  /**
   * Stores {@code rootRecordName} and returns {@code ControllerContext}.
   *
   * <p>See {@link ControllerContext#mainRootRecordName()}.</p>
   *
   * @param rootRecordName rootRecordName
   * @return ControllerContext
   */
  public ControllerContext mainRootRecordName(String rootRecordName) {
    this.mainRootRecordName = rootRecordName;
    return this;
  }

  /**
   * Returns a rootRecordName linked to the controller.
   *
   * <p>The Field name of the record directly defined in the form
   *     normally uses the corresponding entity name.<br>
   *     Especially when you use the list-edit template,
   *     there is a rule that only one record is kept under the form,
   *     so it is recommended to make it the same as the entity name. <br>
   *     (In list-edit template, other patterns are not supported
   *     as they are not necessary and have not been implemented before.)</p>
   *
   * <p>On the java side, recordName is used for automatic completion
   *     when it is troublesome to write it every time. <br>
   *     The value of this field is also passed to the template
   *     on the html side and used with the same parameter name.</p>
   *
   * <p>In the case of page-general, the record may not exist.
   *     In that case, rootRecordName field stored in the controller becomes null.
   *     If not set, the value which is same as function is returned.</p>
   *
   * @return rootRecordName
   */
  public String mainRootRecordName() {
    return mainRootRecordName == null ? function : Objects.requireNonNull(mainRootRecordName);
  }
}
