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
 * Provides constants.
 */
public class SplibWebConstants {

  public static final String KEY_DATA_KIND = "dataKind";

  public static final String KEY_CONTROLLER = "controller";

  public static final String KEY_FORMS = "forms";

  public static final String KEY_MODEL = "model";

  /** Flash attribute key used to carry model attributes across a PRG redirect. */
  public static final String KEY_SAVED_MODEL = "_splibSavedModel";

  /** Model attribute key holding aggregated global errors collected from all BindingResults. */
  public static final String KEY_GLOBAL_ERRORS = "splibGlobalErrors";

  /** Model attribute key holding the warning message that requires user confirmation. */
  public static final String KEY_WARN_MESSAGE = "warnMessage";

  /** Model attribute key holding a boolean flag indicating a success message should be shown. */
  public static final String KEY_NEEDS_SUCCESS_MESSAGE = "needsSuccessMessage";
  
  public static final String KEY_MODEL_MESSAGES_AT_ITEMS = "showsMessagesLinkedToItemsAtEachItem";
  
  public static final String KEY_BASE_PAGE_LOGIN_STATE = "data-show-page-login-state";
}
