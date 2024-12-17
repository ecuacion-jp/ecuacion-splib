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
package jp.ecuacion.splib.web.exceptionhandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.AppWarningException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.BeanValidationUtil;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.web.advice.SplibControllerAdvice;
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.bean.RedirectUrlBean;
import jp.ecuacion.splib.web.bean.SplibModelAttributes;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.BizLogicRedirectAppException;
import jp.ecuacion.splib.web.exception.InputValidationException;
import jp.ecuacion.splib.web.exception.internal.HtmlFileNotAllowedToOpenException;
import jp.ecuacion.splib.web.exception.internal.HtmlFileNotFoundException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.form.record.RecordInterface;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

public abstract class SplibExceptionHandler {

  public static final String INFO_FOR_ERROR_HANDLING = "ecuacion.spring.mvc.infoForErrorHandling";

  private LogUtil logUtil = new LogUtil(this);
  private DetailLogger detailLog = new DetailLogger(this);

  @Autowired
  HttpServletRequest request;

  @Autowired
  SplibExceptionHandlerAction actionOnThrowable;

  @Autowired
  private SplibModelAttributes modelAttr;

  @Autowired
  private SplibUtil util;

  protected SplibGeneralController<?> getController() {
    return (SplibGeneralController<?>) request.getAttribute(SplibWebConstants.KEY_CONTROLLER);
  }

  /** Controller処理前にrequestに格納しておいたmodelを取得。 */
  private Model getModel() {
    return (Model) request.getAttribute(SplibControllerAdvice.REQUEST_KEY_MODEL);
  }

  /** "{0}"がmessageに含まれる場合はitemNameで置き換える。 */
  private String addFieldName(String message, String itemName) {
    return addFieldNames(message, new String[] {itemName});
  }

  private String addFieldNames(String message, String[] itemNames) {
    if (message.contains("{0}")) {
      message = MessageFormat.format(message, getItemNames(itemNames));
    }

    return message;
  }

  private String getItemNames(String[] itemNames) {
    StringBuilder sb = new StringBuilder();
    final String prependParenthesis = PropertyFileUtil.getMsg(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.prependParenthesis");
    final String appendParenthesis = PropertyFileUtil.getMsg(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.appendParenthesis");
    final String separator = PropertyFileUtil.getMsg(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.separator");

    boolean is1stTime = true;
    for (String itemName : itemNames) {

      // itemNameがmessages.propertiesにあったらそれに置き換える
      if (PropertyFileUtil.hasFieldName(itemName)) {
        itemName = PropertyFileUtil.getFieldName(request.getLocale(), itemName);
      }

      if (is1stTime) {
        is1stTime = false;

      } else {
        sb.append(separator);
      }

      sb.append(prependParenthesis + itemName + appendParenthesis);
    }

    return sb.toString();
  }

  @ExceptionHandler({AppWarningException.class})
  public ModelAndView handleAppWarningException(AppWarningException exception,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    MessagesBean requestResult = ((MessagesBean) getModel().getAttribute(MessagesBean.key));

    Objects.requireNonNull(requestResult);
    requestResult.setWarnMessage(exception.getMessageId(), PropertyFileUtil
        .getMsg(request.getLocale(), exception.getMessageId(), exception.getMessageArgs()),
        exception.getButtonId());

    return common(getController(), loginUser);
  }

  @ExceptionHandler({AppException.class})
  public ModelAndView handleAppException(AppException exception,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    RedirectUrlBean redirectBean = null;
    MessagesBean requestResult = ((MessagesBean) getModel().getAttribute(MessagesBean.key));

    // MultipleAppExceptionも考慮し例外を複数持つ
    List<SingleAppException> exList = new ArrayList<>();
    if (exception instanceof MultipleAppException) {
      for (SingleAppException appEx : ((MultipleAppException) exception).getList()) {
        exList.add(appEx);
      }

      // 結果exListがゼロの場合はシステムエラーとして処理。
      if (exList.size() == 0) {
        throw new RuntimeException("No exception included in MultipleAppException.");
      }

    } else {
      exList.add((SingleAppException) exception);
    }

    // exList内のexceptionを一つずつ処理
    for (SingleAppException saex : exList) {

      String[] fields = null;
      // SplibBaseWithRecordController の子でない場合は、そもそもrecord, fieldの定義がlibrary内で確立されていないので処理対象外とする。
      if (saex instanceof BizLogicAppException) {
        BizLogicAppException ex = (BizLogicAppException) saex;
        fields = (ex.getErrorFields() == null) ? new String[] {} : ex.getErrorFields().getFields();

        // fieldsは、html側としてはrootRecordNameから始まる必要があるが、java側からすると毎回書くのは面倒なので、
        // field名のみを記載することを推奨とする。
        // その場合、htmlと整合を取るためにここでrootRecordNameを追加しておく
        List<String> modifiedFieldList = new ArrayList<>();
        Objects.requireNonNull(fields);
        String prefix = getController().getRootRecordName() + ".";
        for (String field : fields) {
          modifiedFieldList.add(field.startsWith(prefix) ? field : (prefix + field));
        }

        fields = modifiedFieldList.toArray(new String[modifiedFieldList.size()]);

        if (ex instanceof BizLogicRedirectAppException) {
          redirectBean = ((BizLogicRedirectAppException) ex).getRedirectUrlBean();
        }
      }

      for (String message : new ExceptionUtil().getAppExceptionMessageList(saex,
          request.getLocale())) {
        // messageは既にmessage.propertiesのメッセージを取得し、パラメータも埋めた状態だが、
        // それでも{0}が残っている場合はfieldsの値を元に項目名を埋める。
        message = addFieldNames(message, fields);
        requestResult.setErrorMessage(message, fields);
      }
    }

    redirectBean =
        redirectBean == null ? getController().getRedirectUrlOnAppExceptionBean() : redirectBean;
    return common(getController(), loginUser, redirectBean);
  }

  /**
   * 楽観的排他制御の処理。 画面表示〜ボタン押下の間にレコード更新された場合は、手動でチェックなので直接BizLogicAppExceptionを投げても良いのだが、
   * 複数sessionで同一レコードを同時更新した場合（service内の処理内でのselectからupdateの間に別sessionがselect〜updateを完了）は
   * JPAが自動でObjectOptimisticLockingFailureExceptionを投げてくるので、それも同様に処理できるよう楽観的排他制御エラーは
   * ObjectOptimisticLockingFailureExceptionで統一しておく。
   * 尚、jpa以外でも、jdbcやfileでのlockでも同様の事象があるので全て統一しObjectOptimisticLockingFailureExceptionを使用するものとする。
   */
  @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
  public ModelAndView handleObjectOptimisticLockingFailureException(
      ObjectOptimisticLockingFailureException exception,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    // 通常のチェックエラー扱いとする
    return handleAppException(
        new BizLogicAppException("jp.ecuacion.splib.web.common.message.optimisticLocking"),
        loginUser);
  }

  @ExceptionHandler({PessimisticLockingFailureException.class})
  public ModelAndView handlePessimisticLockingFailureException(
      PessimisticLockingFailureException exception, @AuthenticationPrincipal UserDetails loginUser)
      throws Exception {
    // 通常のチェックエラー扱いとする
    return handleAppException(
        new BizLogicAppException("jp.ecuacion.splib.web.common.message.pessimisticLocking"),
        loginUser);
  }

  /**
   * 本来はspring mvcのvalidationに任せれば良いのだが、以下の2点が気に入らず、springで持っているerror情報(*)を    *
   * 修正しようとしたが「unmodifiablelist」で修正できなかったため、やむなく個別の処理を作成。
   * 
   * <ol>
   * <li>画面上部に複数項目のエラー情報をまとめて表示する場合、並び順が実行するたびに変わる</li>
   * <li>@Sizeなどのvalidationで、blankを無視する設定になっていないため、
   * 値がblankで、かつblankがエラーのannotationが付加されている場合でも、@Sizeなどのvalidationが走ってしまう。</li>
   * </ol>
   * 
   * <p>
   * 尚、controller層でのinput validationが漏れた場合、JPAの仕様によりDBアクセス直前にもう一度validationが行われ、
   * そこでConstraintViolationExceptionが発生する。
   * そうなる場合は、事前のチェックが漏れた場合のみであり、また単体のConstraintViolationExceptionしか取得できず、 想定しているエラー表示方法（input
   * validation分はまとめて結果を表示）とは異なり正しくない。
   * そのため実装不備とみなしシステムエラーとする（＝本クラス上で特別扱いはせず、"handleThrowable"methodで拾う）。 <br>
   * I(*) このような形でspring mvcのerror情報の取得が可能。最後のprintlnではfieldとcode（validator名："Size"など）を取得している。
   * </p>
   * 
   * <p>
   * // bean validation標準の各種validatorが、""の場合でもSize validatorのエラーが表示されるなどイマイチ。 //
   * 空欄の場合には空欄のエラーのみを出したいので、同一項目で、NotEmptyと別のvalidatorが同時に存在する場合はNotEmpty以外を間引く。 //
   * spring標準のvalidatorは、model内に以下のようなkey名で格納されているのでkey名を前方一致で取得 //
   * org.springframework.validation.BindingResult.driveRecordEditForm String bindingResultKey =
   * getModel().asMap().keySet().stream() .filter(key ->
   * key.startsWith("org.springframework.validation.BindingResult"))
   * .collect(Collectors.toList()).get(0); BeanPropertyBindingResult bindingResult =
   * (BeanPropertyBindingResult) getModel().asMap().get(bindingResultKey); for (FieldError error :
   * bindingResult.getFieldErrors()) { System.out.println(error.getCode());
   * System.out.println(error.getField()); }
   * </p>
   */
  @ExceptionHandler({InputValidationException.class})
  public ModelAndView handleInputValidationException(InputValidationException exception,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    final MessagesBean requestResult = ((MessagesBean) getModel().getAttribute(MessagesBean.key));

    // spring mvcでのvalidation結果からは情報がうまく取れないので、改めてvalidationを行う
    List<ValidationErrorInfoBean> errorList = 
        new BeanValidationUtil().validate(exception.getForm(), request.getLocale()).stream()
            .map(cv -> new ValidationErrorInfoBean(cv)).collect(Collectors.toList());

    // NotEmptyのチェック結果を追加
    errorList.addAll(exception.getForm()
        .validateNotEmpty(request.getLocale(), util.getLoginState(),
            new SplibSecurityUtil().getRolesAndAuthoritiesBean(loginUser))
        .stream().collect(Collectors.toList()));

    // 並び順を指定。今後項目名指定することも想定するが、一旦は並び順が固定されれば満足なので単純に並べる
    errorList = errorList.stream().sorted(getComparator()).collect(Collectors.toList());

    // bean validation標準の各種validatorが、""の場合でもSize validatorのエラーが表示されるなどイマイチ。
    // 空欄の場合には空欄のエラーのみを出したいので、同一項目で、NotEmptyと別のvalidatorが同時に存在する場合はNotEmpty以外を間引く。
    removeDuplicatedValidators(errorList);

    // ここでerrorList.size() == 0のパターンが発生。
    // spring側のvalidationではresultにエラー内容が格納されたが自分でvalidateしなおすとエラー検知できない、というパターン。
    // systemErrorとしておく
    if (errorList.size() == 0) {
      String msg = """
          SplibExceptionHandler#handleInputValidationExceptionに来ているのにerrorListにエラー項目が存在しません。
          rootRecoordNameと同一のnameを持ったsubmitボタンを押した場合に発生する模様。
          （例：rootRecordNameをappとした場合、以下のようなボタンを作成する場合に発生）
          <div th:replace="~{bootstrap/components :: commonPrimaryButton('app', ...)}"></div>

          textなどの情報は、GETでいうと、request paramの中で"app.desc=..."のように記載されるが、そのparameterの中で
          spring mvcだとボタン名が"app="と出る。
          このボタン名に対する"app="を、"app"のキーワードがあるためformへのmapping対象だとspringが勘違いするために、
          mappingをトライしエラーが発生しているのかも。
          （ちなみに、ボタンのnameがappzzzなど、appから始まるがappとは異なる文字列であれば、この問題は発生しない）
          """;
      throw new RuntimeException(msg);
    }

    // messageを設定。{0}を項目名で埋める、などはspring mvcの機能でもやっているが、それだと使いにくいので別途実施
    for (ValidationErrorInfoBean cv : errorList) {

      String message = cv.getMessage();
      String itemId = cv.getPropertyPath().toString();

      String itemName = itemId;
      String recordName = itemName.substring(0, itemName.indexOf("."));
      itemName = ((RecordInterface) exception.getForm().getRootRecord(recordName))
          .getLabelItemName(recordName, itemName);

      message = addFieldName(message, itemName);

      requestResult.setErrorMessage(message, itemId);
    }

    return common(getController(), loginUser);
  }

  /**
   * bean validation標準の各種validatorが、""の場合でもSize validatorのエラーが表示されるなどイマイチ。
   * 空欄の場合には空欄のエラーのみを出したいので、同一項目で、NotEmptyと別のvalidatorが同時に存在する場合はNotEmpty以外を間引く。
   */
  private void removeDuplicatedValidators(List<ValidationErrorInfoBean> cvList) {

    // 一度、field名をkey, valueをsetとしcvを複数格納するmapを作成し、そのkeyに2件以上validatorが存在しかつNotEmptyが存在する場合は
    // NotEmpty以外を間引く、というロジックとする
    Map<String, Set<ValidationErrorInfoBean>> duplicateCheckMap = new HashMap<>();

    // 後続処理のため、NotEmptyを保持するkeyを保持しておく
    Set<String> keySetWithNotEmpty = new HashSet<>();

    for (ValidationErrorInfoBean cv : cvList) {
      String key = cv.getPropertyPath().toString();
      if (duplicateCheckMap.get(key) == null) {
        duplicateCheckMap.put(key, new HashSet<>());
      }

      duplicateCheckMap.get(key).add(cv);

      // NotEmptyの場合はkeySetWithNotEmptyに追加
      if (isNotEmptyValidator(cv)) {
        keySetWithNotEmpty.add(key);
      }
    }

    // duplicateCheckMapで、keySetWithNotEmptyに含まれるkeyのvalueは、NotEmpty以外を取り除く
    for (String key : keySetWithNotEmpty) {
      for (ValidationErrorInfoBean cv : duplicateCheckMap.get(key)) {
        if (!isNotEmptyValidator(cv)) {
          cvList.remove(cv);
        }
      }
    }
  }

  private boolean isNotEmptyValidator(ValidationErrorInfoBean cv) {
    String validatorClass = cv.getValidatorClass();
    return validatorClass.endsWith("NotEmpty") || validatorClass.endsWith("NotEmptyIfValid");
  }

  private Comparator<ValidationErrorInfoBean> getComparator() {
    return new Comparator<>() {
      @Override
      public int compare(ValidationErrorInfoBean f1, ValidationErrorInfoBean f2) {
        // 項目名で比較
        int result = f1.getPropertyPath().toString().compareTo(f2.getPropertyPath().toString());
        if (result != 0) {
          return result;
        }

        // 項目名が同じ場合はmessageTemplate(実質はvalidator種別）で比較
        result = f1.getValidatorClass().compareTo(f2.getValidatorClass());
        if (result != 0) {
          return result;
        }

        // validator種別も同じ場合は、@Patternのみと考えられる。
        // Patternの場合はregxpにより並び順が固定されるのでそれを含む文字列で比較
        String s1 = f1.getAnnotationDescriptionString();
        String s2 = f2.getAnnotationDescriptionString();
        return s1.compareTo(s2);
      }
    };
  }

  @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
  public String handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException exception, Model model) throws BizLogicAppException {

    logUtil.logError(exception, request.getLocale());
    actionOnThrowable.execute(exception);

    return "redirect:/" + PropertyFileUtil.getApp("jp.ecuacion.splib.web.system-error.go-to-path");
  }

  /**
   * HTTP 404。
   */
  @ExceptionHandler({NoResourceFoundException.class})
  public void handleNoResourceFoundException(NoResourceFoundException exception, Model model)
      throws NoResourceFoundException {
    // 後述のhandleThrowableに含まれないよう別出ししているが、特に処理せずそのまま投げることで
    // spring標準のエラー処理（error/404.htmlを参照）の動きとする
    throw exception;
  }

  /**
   * ShowPageControllerにおいて、htmlFileが存在しなかった場合に発生。
   * userが自由にurlパラメータを設定できるため、システムエラーになるのは微妙なことから別処理としておく。
   */
  @ExceptionHandler({HtmlFileNotFoundException.class})
  ModelAndView handleHtmlFileNotFoundException(HtmlFileNotFoundException exception, Model model)
      throws NoResourceFoundException {

    detailLog.info("Designated html file not found. html file name = " + exception.getFileName());

    // それっぽいURLにredirectしておく。なければさらにredirectされて適切な画面が表示される。
    return new ModelAndView("redirect:/public/home/page");
  }

  /**
   * ShowPageControllerにおいて、htmlFileに表示を許す設定がされていなかった場合に発生。
   */
  @ExceptionHandler({HtmlFileNotAllowedToOpenException.class})
  ModelAndView handleHtmlFileNotAllowedToOpenException(HtmlFileNotAllowedToOpenException exception,
      Model model) throws NoResourceFoundException {

    detailLog.info("Designated html file not allowed to open. Needs to add option to html tag."
        + " html file name = " + exception.getFileName());

    // それっぽいURLにredirectしておく。なければさらにredirectされて適切な画面が表示される。
    return new ModelAndView("redirect:/public/home/page");
  }

  /**
   * WebでConstraintViolationExceptionが直接上がってくる場面は、事前のチェックが漏れた場合であり、
   * この場合は単体のConstraintViolationExceptionしか取得できないこともあり正しくないため、実装不備とみなしここに入れることとする。
   */
  @ExceptionHandler({Throwable.class})
  public ModelAndView handleThrowable(Throwable exception, Model model) {

    logUtil.logError(exception, request.getLocale());

    // 個別appの処理。mail送信など。
    actionOnThrowable.execute(exception);

    Model mdl = getModel() == null ? model : getModel();
    modelAttr.addAllToModel(mdl);

    return new ModelAndView("error", mdl.asMap(), HttpStatusCode.valueOf(500));
  }

  private ModelAndView common(SplibGeneralController<?> ctrl, UserDetails loginUser)
      throws Exception {
    return common(ctrl, loginUser, ctrl.getPrepareSettings().isRedirect(),
        ctrl.getRedirectUrlOnAppExceptionBean());
  }

  private ModelAndView common(SplibGeneralController<?> ctrl, UserDetails loginUser,
      RedirectUrlBean redirectBean) throws Exception {
    return common(ctrl, loginUser, true, redirectBean);
  }

  private ModelAndView common(SplibGeneralController<?> ctrl, UserDetails loginUser,
      boolean isRedirect, RedirectUrlBean redirectBean) throws Exception {

    SplibGeneralForm[] forms =
        (SplibGeneralForm[]) request.getAttribute(SplibWebConstants.KEY_FORMS);

    // #603:
    // 本当は、ExceptionHandlerのエラー処理の中でも、redirectしない場合のみprepareFormを呼べばいいはずなのだが
    // そこのhandlingはまだできていない。必要時に整理。
    getController().getService().prepareForm(Arrays.asList(forms), loginUser);

    // noredirectの有無で分岐
    if (!isRedirect) {
      return new ModelAndView(ctrl.getDefaultHtmlFileName(), getModel().asMap());

    } else {
      // redirectBean == nullの場合は自画面遷移、自画面遷移の場合はmodelの情報も保持する
      String path = util.prepareForRedirectAndGetPath(redirectBean, redirectBean == null, ctrl,
          request, getModel());

      return new ModelAndView(path);
    }
  }

  /**
   * NotEmptyの効率的な実装のために動的なNotEmptyの適用（BaseRecordに@NotEmptyを記載しておき、何らかの条件でそれをactiveにする形）を検討したが、
   * 結果不可であることがわかった。その場合、個別ロジックでnot emptyチェックを行右必要があるが、別途実施されたbean validationの結果と合わせる。 その際、実際のbean
   * validationの結果（ConstraintViolation）を自分で生成するのはなかなか辛いので、bean validationの結果も含め
   * 必要な項目を本beanに入れて後処理を共通化する
   */
  public static class ValidationErrorInfoBean {
    private String message;
    private String propertyPath;
    private String validatorClass;

    private String annotationDescriptionString;

    /** こちらはnotEmptyのロジックチェック側で使用。 */
    public ValidationErrorInfoBean(String message, String propertyPath, String validatorClass) {
      this.message = message;
      this.propertyPath = propertyPath;
      this.validatorClass = validatorClass;

      // これは@Pattern用なので実質使用はしないのだが、nullだとcompareの際におかしくなると嫌なので空白にしておく
      annotationDescriptionString = "";
    }

    public ValidationErrorInfoBean(ConstraintViolation<?> cv) {
      this.message = cv.getMessage();
      this.propertyPath = cv.getPropertyPath().toString();
      this.validatorClass = cv.getConstraintDescriptor().getAnnotation().annotationType().getName();
      this.annotationDescriptionString = cv.getConstraintDescriptor().getAnnotation().toString();
    }

    public String getMessage() {
      return message;
    }

    public String getPropertyPath() {
      return propertyPath;
    }

    public String getValidatorClass() {
      return validatorClass;
    }

    public String getAnnotationDescriptionString() {
      return annotationDescriptionString;
    }
  }
}
