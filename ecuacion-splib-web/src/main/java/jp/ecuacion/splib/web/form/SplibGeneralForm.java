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
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.jakartavalidation.bean.ConstraintViolationBean;
import jp.ecuacion.splib.core.record.SplibRecord;
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
    public BindingResult bindingResult() {
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
  public SplibGeneralForm validate(BindingResult bindingResult) {
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
   * 一つの機能の実装を複数メニューで使い回す場合に使用するメニュー名.
   * 
   * <p>使い回しをしない通常機能は、放置しnull（実際にはhtml側でhiddenを持っているのでそれがsubmitされ空文字になる）のままでよい。
   * searchFormはsessionにformを保持し検索条件を保持するが、複数メニューで使い回す場合は
   * そのメニュー別に検索条件を保持するのが望ましいため、このdataKindもsession保存時のキーとする。
   * </p>
   * 
   * <p>現行設計ではdataKindをrequestで持ち回る形を採用している。 sessionに持たせた方が実装が楽と思われる部分もありながら、navBarから他のメニューを選択された時には
   * sessionに保管されたdataKindではなくrequestの方のdataKindを採用する、あたりをうまく回す必要があり、
   * その意味ではsessionに持たせるよりrequestで持ち回る方がわかりやすい（dataKindが空ならシステムエラーになるので）ことから
   * まずはrequestで持ち回り、の実装方式としておく。 ※PRGを使用しGETでのアクセスを可としている中で、個々のURLをコピって使用しても使用可能とする意味でも、
   * 全てのURLにdataKindパラメータがついているのが望ましい、という観点もあるので付記。
   * </p>
   */
  protected String dataKind;

  /**
   * warningを返した際に、それに対してOKした場合は、OKしたという履歴を残さないとまた再度同じチェックに引っかかりワーニングを出してしまう。
   * それを防ぐため、一度OKしたwarningは、messageIdを本fieldに保管することで避ける.
   * 
   * <p>複数のwarnに対する情報を保持できるよう、本項目はcsv形式とする。（対応するhiddenにjavascript側で","とメッセージIDを追加）</p>
   */
  protected String confirmedWarnings;

  public String getDataKind() {
    return dataKind;
  }

  public void setDataKind(String dataKind) {
    this.dataKind = dataKind;
  }

  private ControllerContext controllerContext;

  public ControllerContext getControllerContext() {
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
   * form配下に存在するrecordを全て取得。 戻り値のmapのkeyはfield名.
   */
  public List<Field> getRootRecordFields() {
    // form内に保持しているrecordを取得。
    // 複数functionで同一の画面を使う場合に一方のformを継承した場合、親classがprivateで持っているfieldを取得する必要があるため
    // ループを回して親に遡っての捜索となっている
    Class<?> formCls = this.getClass();
    List<Field> checkList = new ArrayList<>();
    while (true) {
      Field[] fields = formCls.getDeclaredFields();

      // 正しくrecordが存在しているかのチェックとrecordの取得
      for (Field field : fields) {
        if (SplibRecord.class.isAssignableFrom(field.getType())) {
          checkList.add(field);
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
  public Object getRootRecord(String recordName) {
    Field field = getRootRecordFields().stream().collect(Collectors.toMap(f -> f.getName(), f -> f))
        .get(recordName);
    return getRootRecord(field);
  }

  /**
   * Gets root record.
   */
  protected Object getRootRecord(Field rootRecordField) {
    rootRecordField.setAccessible(true);

    Object rootRecord = null;

    try {
      rootRecord = (Object) rootRecordField.get(this);

    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return rootRecord;
  }

  public String getConfirmedWarnings() {
    return confirmedWarnings;
  }

  /**
   * Returns boolean whether specified messageId is contained.
   * 
   * @param messageId messageId
   * @return boolean
   */
  public boolean containsConfirmedWarning(String messageId) {
    return getConfirmedWarningMessageSet().size() > 0;
  }

  /**
   * Returns already confirmed message ID set.
   * @return {@code Set<String>}
   */
  public Set<String> getConfirmedWarningMessageSet() {
    Set<String> rtnSet = new HashSet<>();

    if (confirmedWarnings == null || confirmedWarnings.equals("")) {
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
  public boolean hasNotEmptyError(String loginState, RolesAndAuthoritiesBean bean) {
    return validateNotEmpty(loginState, bean).size() > 0;
  }

  /**
   * Validates notEmpty.
   */
  public Set<ConstraintViolationBean<SplibGeneralForm>> validateNotEmpty(String loginState,
      RolesAndAuthoritiesBean bean) {
    return validateNotEmpty(Locale.getDefault(), loginState, bean);
  }

  /**
   * Validates notEmpty.
   */
  public Set<ConstraintViolationBean<SplibGeneralForm>> validateNotEmpty(Locale locale,
      String loginState, RolesAndAuthoritiesBean bean) {

    final String validationClass = "jakarta.validation.constraints.NotEmpty";
    Set<ConstraintViolationBean<SplibGeneralForm>> rtnSet = new HashSet<>();

    List<Field> rootRecordFieldList = getRootRecordFields();
    for (Field rootRecordField : rootRecordFieldList) {
      String rootRecordName = rootRecordField.getName();
      HtmlItemContainer rootRecord = (HtmlItemContainer) getRootRecord(rootRecordField);

      for (String notEmptyItemPropertyPath : getNotEmptyItemPropertyPathList(rootRecord, loginState,
          bean)) {
        Object value = ((SplibRecord) rootRecord).getValue(notEmptyItemPropertyPath);

        if (value == null || (value instanceof String && ((String) value).equals(""))) {
          rtnSet
              .add(new ConstraintViolationBean<SplibGeneralForm>(this, validationClass + ".message",
                  validationClass, rootRecordName, notEmptyItemPropertyPath));
        }
      }
    }

    return rtnSet;
  }

  /**
   * Supplies NotEmpty fields in rootRecord.
   * 
   * <p>This became a independent method because some form doesn't use "notEmpty()".
   *     For example, searchForm uses "notEmptyOnSearch()", not "notEmpty()".</p>
   */
  protected List<String> getNotEmptyItemPropertyPathList(HtmlItemContainer rootRecord,
      String loginState, RolesAndAuthoritiesBean bean) {
    return rootRecord.getNotEmptyItemPropertyPathList(loginState, bean);
  }
}
