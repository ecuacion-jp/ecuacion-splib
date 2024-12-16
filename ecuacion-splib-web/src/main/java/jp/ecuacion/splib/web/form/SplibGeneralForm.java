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
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import jp.ecuacion.splib.core.form.record.SplibRecord;
import jp.ecuacion.splib.web.controller.SplibGeneralController.ControllerContext;
import jp.ecuacion.splib.web.exceptionhandler.SplibExceptionHandler;
import jp.ecuacion.splib.web.form.record.RecordInterface;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import org.springframework.validation.BindingResult;

public abstract class SplibGeneralForm {

  public static class PrepareSettings {

    /** defaultはvalidationなし。 */
    private boolean validates = false;
    private BindingResult bindingResult;

    public boolean validates() {
      return validates;
    }

    public BindingResult bindingResult() {
      return bindingResult;
    }
  }

  private PrepareSettings prepareSettings = new PrepareSettings();

  public PrepareSettings getPrepareSettings() {
    return prepareSettings;
  }

  /** prepare method内でのvalidation要否判断に使用。 */
  public SplibGeneralForm validate(BindingResult bindingResult) {
    prepareSettings.validates = true;
    prepareSettings.bindingResult = bindingResult;
    return this;
  }

  /** prepare method内でのvalidation要否判断に使用。 */
  public SplibGeneralForm noValidate() {
    prepareSettings.validates = false;
    return this;
  }

  /**
   * 一つの機能の実装を複数メニューで使い回す場合に使用するメニュー名。
   * 
   * <p>
   * 使い回しをしない通常機能は、放置しnull（実際にはhtml側でhiddenを持っているのでそれがsubmitされ空文字になる）のままでよい。
   * searchFormはsessionにformを保持し検索条件を保持するが、複数メニューで使い回す場合は
   * そのメニュー別に検索条件を保持するのが望ましいため、このdataKindもsession保存時のキーとする。
   * </p>
   * 
   * <p>
   * 現行設計ではdataKindをrequestで持ち回る形を採用している。 sessionに持たせた方が実装が楽と思われる部分もありながら、navBarから他のメニューを選択された時には
   * sessionに保管されたdataKindではなくrequestの方のdataKindを採用する、あたりをうまく回す必要があり、
   * その意味ではsessionに持たせるよりrequestで持ち回る方がわかりやすい（dataKindが空ならシステムエラーになるので）ことから
   * まずはrequestで持ち回り、の実装方式としておく。 ※PRGを使用しGETでのアクセスを可としている中で、個々のURLをコピって使用しても使用可能とする意味でも、
   * 全てのURLにdataKindパラメータがついているのが望ましい、という観点もあるので付記。
   * </p>
   */
  protected String dataKind;

  /**
   * warningを返した際に、それに対してOKした場合は、OKしたという履歴を残さないとまた再度同じチェックに引っかかりワーニングを出してしまう。
   * それを防ぐため、一度OKしたwarningは、messageIdを本fieldに保管することで避ける。
   * 複数のwarnに対する情報を保持できるよう、本項目はcsv形式とする。（対応するhiddenにjavascript側で","とメッセージIDを追加）
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

  /** 引数は、"acc"のような形で渡される想定。 */
  public SplibRecord get(String itemName) throws IllegalArgumentException, IllegalAccessException,
      NoSuchFieldException, SecurityException {
    return (SplibRecord) this.getClass().getField(itemName).get(this);
  }

  /**
   * form配下に存在するrecordを全て取得。 戻り値のmapのkeyはfield名。
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

  /** form配下にrecordが1つしかない前提で使用されるメソッド。deprecatedにしたいのだが、状況確認中。 */
  @Deprecated
  public Field getRootRecordField() {
    List<Field> checkList = getRootRecordFields();

    if (checkList.size() == 0) {
      throw new RuntimeException(
          "SplibRecord field not found in form: " + this.getClass().getName());

    } else if (checkList.size() > 1) {
      throw new RuntimeException(
          "Multiple SplibRecord field found in form: " + this.getClass().getName());

    } else {
      return checkList.get(0);
    }
  }

  /** formからrecordを取得する処理。 */
  @Deprecated
  public Object getRootRecord() {
    return getRootRecord(getRootRecordField());
  }

  /** formからrecordを取得する処理。 */
  public Object getRootRecord(String recordName) {
    Field field = getRootRecordFields().stream().collect(Collectors.toMap(f -> f.getName(), f -> f))
        .get(recordName);
    return getRootRecord(field);
  }

  /** getLabelItemName() ではfieldも別途使用するため、fieldの二回取得は無駄なのでfieldを引数にするメソッドも作っておく。 */
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

  public boolean containsConfirmedWarning(String messageId) {
    return getConfirmedWarningMessageSet().size() > 0;
  }

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

  /** EditFormのメソッドを呼び出すことで、その中の全てのNotEmpty Checkを実施。 */
  public boolean hasNotEmptyError(String loginState, RolesAndAuthoritiesBean bean) {
    return validateNotEmpty(loginState, bean).size() > 0;
  }

  /** NotEmptyエラーの有無・件数だけを知りたい場合で、localeを取得するのが面倒な際はこちらを使用。 */
  public Set<SplibExceptionHandler.ValidationErrorInfoBean> validateNotEmpty(String loginState,
      RolesAndAuthoritiesBean bean) {
    return validateNotEmpty(Locale.getDefault(), loginState, bean);
  }

  /** NotEmptyエラーのメッセージを取得したい場合はこちらを使用。 */
  public Set<SplibExceptionHandler.ValidationErrorInfoBean> validateNotEmpty(Locale locale,
      String loginState, RolesAndAuthoritiesBean bean) {

    final String validationClass = "jakarta.validation.constraints.NotEmpty";
    Set<SplibExceptionHandler.ValidationErrorInfoBean> rtnSet = new HashSet<>();

    List<Field> rootRecordFieldList = getRootRecordFields();
    for (Field rootRecordField : rootRecordFieldList) {
      String rootRecordFieldName = rootRecordField.getName();
      RecordInterface rootRecord = (RecordInterface) getRootRecord(rootRecordField);

      for (String notEmptyField : rootRecord.getNotEmptyFields(loginState, bean)) {
        Object value = ((SplibRecord) rootRecord).getValue(notEmptyField);

        if (value == null || (value instanceof String && ((String) value).equals(""))) {
          rtnSet.add(new SplibExceptionHandler.ValidationErrorInfoBean(
              ResourceBundle.getBundle("ValidationMessages", locale)
                  .getString(validationClass + ".message"),
              rootRecordFieldName + "." + notEmptyField, validationClass));
        }
      }
    }

    return rtnSet;
  }
}
