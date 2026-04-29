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
package jp.ecuacion.splib.web.form;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.core.record.SplibRecord;
import org.jspecify.annotations.Nullable;
import jp.ecuacion.splib.web.controller.SplibGeneralController.ControllerContext;
import jp.ecuacion.splib.web.item.HtmlItemContainer;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import org.springframework.validation.BindingResult;

/**
 * Stores data for generalController.
 */
public abstract class SplibGeneralForm {

  /**
   * Stores common info.
   */
  public static class PrepareSettings {

    private boolean validates = false;
    @Nullable
    private BindingResult bindingResult;

    /**
     * Returns whether validation is executed or not.
     * 
     * @return boolean
     */
    public boolean validates() {
      return validates;
    }

    /**
     * Returns bindingResult.
     * 
     * @return bindingResult
     */
    public @Nullable BindingResult bindingResult() {
      return bindingResult;
    }
  }

  private PrepareSettings prepareSettings = new PrepareSettings();

  public PrepareSettings getPrepareSettings() {
    return prepareSettings;
  }

  /**
   * Sets validates = true and bindingResult, and returns this for method chain.
   */
  public SplibGeneralForm validate(@Nullable BindingResult bindingResult) {
    prepareSettings.validates = true;
    prepareSettings.bindingResult = bindingResult;
    return this;
  }

  /**
   * Sets vavlidates = false and returns this for method chain.
   */
  public SplibGeneralForm noValidate() {
    prepareSettings.validates = false;
    return this;
  }

  /**
   * Menu name used when a single function implementation is reused across multiple menus.
   *
   * <p>For normal functions that are not reused, leave this null
   * (in practice the html side has a hidden input which submits an empty string).
   * Since searchForm retains the form in the session to hold search conditions,
   * when reusing across multiple menus it is preferable to retain search conditions
   * per menu, so this dataKind is also used as the key when saving to session.
   * </p>
   *
   * <p>The current design passes dataKind via the request.
   * Although storing it in the session would simplify some parts of the implementation,
   * when a different menu is selected from the navBar the dataKind from the request must be
   * used instead of the one saved in the session, which requires careful handling.
   * In that sense, passing it via request is clearer than storing in session
   * (because an empty dataKind causes a system error).
   * Therefore the implementation uses request-based passing for now.
   * Note: since PRG is used and GET access is allowed, having the dataKind parameter in every
   * URL is also desirable so that copying and using individual URLs still works.
   * </p>
   */
  @Nullable
  protected String dataKind;

  /**
   * When a warning is returned and the user confirms it with OK,
   * without recording that history the same check would trigger again and show the warning.
   * To prevent this, once-confirmed warnings are stored by their messageId in this field.
   *
   * <p>To hold information for multiple warnings, this field uses CSV format.
   * (The corresponding hidden input appends "," and the message ID on the JavaScript side.)
   * </p>
   */
  @Nullable
  protected String confirmedWarnings;

  public @Nullable String getDataKind() {
    return dataKind;
  }

  public void setDataKind(String dataKind) {
    this.dataKind = dataKind;
  }

  @Nullable
  private ControllerContext controllerContext;

  public @Nullable ControllerContext getControllerContext() {
    return controllerContext;
  }

  public void setControllerContext(ControllerContext controllerContext) {
    this.controllerContext = controllerContext;
  }

  /**
   * Obtains record.
   */
  public SplibRecord get(String itemName) throws IllegalArgumentException, IllegalAccessException,
      NoSuchFieldException, SecurityException {
    return (SplibRecord) this.getClass().getField(itemName).get(this);
  }

  /**
   * Gets all records under the form. The key of the returned map is the field name.
   */
  public List<Field> getRootRecordFields() {
    // Gets records held in the form.
    // When one form is extended for use on the same screen with multiple functions,
    // private fields in the parent class must also be retrieved,
    // so the search traverses up the class hierarchy in a loop.
    Class<?> formCls = this.getClass();
    List<Field> checkList = new ArrayList<>();
    while (true) {
      Field[] fields = formCls.getDeclaredFields();

      // Check that a record correctly exists and retrieve it.
      for (Field field : fields) {
        if (SplibRecord.class.isAssignableFrom(field.getType())) {
          checkList.add(field);
          field.setAccessible(true);
        }
      }

      if (formCls.getSuperclass() == SplibGeneralForm.class) {
        return checkList;

      } else {
        formCls = formCls.getSuperclass();

      }
    }
  }

  /**
   * Gets root record.
   */
  public @Nullable Object getRootRecord(String recordName) {
    Field field = getRootRecordFields().stream()
        .filter(f -> f.getName().equals(recordName)).findFirst().orElse(null);
    if (field == null) {
      return null;
    }
    return getRootRecord(field);
  }

  /**
   * Gets root record.
   */
  protected @Nullable Object getRootRecord(Field rootRecordField) {
    rootRecordField.setAccessible(true);

    Object rootRecord = null;

    try {
      rootRecord = rootRecordField.get(this);

    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return rootRecord;
  }

  public @Nullable String getConfirmedWarnings() {
    return confirmedWarnings;
  }

  /**
   * Returns boolean whether specified messageId is contained.
   * 
   * @param messageId messageId
   * @return boolean
   */
  public boolean containsConfirmedWarning(String messageId) {
    return getConfirmedWarningMessageSet().contains(messageId);
  }

  /**
   * Returns already confirmed message ID set.
   * @return {@code Set<String>}
   */
  public Set<String> getConfirmedWarningMessageSet() {
    Set<String> rtnSet = new HashSet<>();

    if (confirmedWarnings == null || confirmedWarnings.isEmpty()) {
      return rtnSet;
    }

    rtnSet.addAll(Arrays.asList(confirmedWarnings.split(",")));
    return rtnSet;
  }

  public void setConfirmedWarnings(String confirmedWarnings) {
    this.confirmedWarnings = confirmedWarnings;
  }

  /**
   * Has a notEmpty error.
   */
  public boolean hasNotEmptyError(Object rootBean, String loginState,
      @Nullable RolesAndAuthoritiesBean bean) {
    Violations violations = new Violations();
    validateNotEmpty(rootBean, violations, loginState, bean);
    return !violations.getBusinessViolations().isEmpty()
        || !violations.getConstraintViolations().isEmpty();
  }

  /**
   * Validates notEmpty.
   */
  public void validateNotEmpty(Object rootBean, Violations violations, String loginState,
      @Nullable RolesAndAuthoritiesBean bean) {
    validateNotEmpty(rootBean, violations, Locale.getDefault(), loginState, bean);
  }

  /**
   * Validates notEmpty.
   */
  public void validateNotEmpty(Object rootBean, Violations violations, Locale locale,
      String loginState, @Nullable RolesAndAuthoritiesBean bean) {

    final String validationClass = "jakarta.validation.constraints.NotEmpty";
    // Set<ConstraintViolationBean<SplibGeneralForm>> rtnSet = new HashSet<>();

    List<Field> rootRecordFieldList = getRootRecordFields();
    for (Field rootRecordField : rootRecordFieldList) {
      // String rootRecordName = rootRecordField.getName();
      HtmlItemContainer rootRecord = (HtmlItemContainer) getRootRecord(rootRecordField);

      if (rootRecord == null) {
        continue;
      }
      for (String notEmptyItemPropertyPath : getNotEmptyItemPropertyPathList(rootRecord, loginState,
          bean)) {
        Object value = ((SplibRecord) rootRecord).getValue(notEmptyItemPropertyPath);

        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
          violations.add(new BusinessViolation(rootBean, new String[] {notEmptyItemPropertyPath},
              validationClass + ".message"));
          // rtnSet.add(new ConstraintViolationBean<SplibGeneralForm>(validationClass, this,
          // "(empty)",
          // validationClass + ".message", rootRecordName + "." + notEmptyItemPropertyPath));
        }
      }
    }
  }

  /**
   * Supplies NotEmpty fields in rootRecord.
   * 
   * <p>This became a independent method because some form doesn't use "notEmpty()".
   *     For example, searchForm uses "notEmptyOnSearch()", not "notEmpty()".</p>
   */
  protected List<String> getNotEmptyItemPropertyPathList(HtmlItemContainer rootRecord,
      String loginState, @Nullable RolesAndAuthoritiesBean bean) {
    if (rootRecord == null || bean == null) {
      return new ArrayList<>();
    }
    return rootRecord.getNotEmptyItemPropertyPathList(loginState, bean);
  }
}
