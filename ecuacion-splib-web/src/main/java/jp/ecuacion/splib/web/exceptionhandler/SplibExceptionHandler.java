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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import java.nio.channels.OverlappingFileLockException;
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
import jp.ecuacion.lib.core.annotation.RequireNonnull;
import jp.ecuacion.lib.core.beanvalidation.bean.BeanValidationErrorInfoBean;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.AppWarningException;
import jp.ecuacion.lib.core.exception.checked.BeanValidationAppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.BeanValidationUtil;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.web.bean.HtmlItem;
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.bean.ReturnUrlBean;
import jp.ecuacion.splib.web.bean.SplibModelAttributes;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.BizLogicRedirectAppException;
import jp.ecuacion.splib.web.exception.FormInputValidationException;
import jp.ecuacion.splib.web.exception.HtmlFileNotAllowedToOpenException;
import jp.ecuacion.splib.web.exception.HtmlFileNotFoundException;
import jp.ecuacion.splib.web.exception.WebAppWarningException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.form.record.RecordInterface;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Provides exception handler.
 */
public abstract class SplibExceptionHandler {

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

  /**
   * Returns the controller from which the exception throws.
   * 
   * @return SplibGeneralController
   */
  @Nonnull
  protected SplibGeneralController<?> getController() {
    return (SplibGeneralController<?>) getModel().getAttribute(SplibWebConstants.KEY_CONTROLLER);
  }

  /**
   * Returns the model obtained at the controller.
   * 
   * @return Model
   */
  @Nonnull
  private Model getModel() {
    return (Model) request.getAttribute(SplibWebConstants.KEY_MODEL);
  }

  /*
   * "{0}"がmessageに含まれる場合はitemNameで置き換える。
   */
  @Nonnull
  private String addFieldName(@RequireNonnull String message, @RequireNonnull String itemName) {
    return addFieldNames(message, new String[] {itemName});
  }

  /*
   * "{0}"がmessageに含まれる場合はitemName（複数項目ある場合は複数項目）で置き換える。
   */
  @Nonnull
  private String addFieldNames(@RequireNonnull String message, @RequireNonnull String[] itemNames) {
    if (message.contains("{0}")) {
      message = MessageFormat.format(message, getItemNames(itemNames));
    }

    return message;
  }

  @Nonnull
  private String getItemNames(@RequireNonnull String[] itemNames) {
    StringBuilder sb = new StringBuilder();
    final String prependParenthesis = PropertyFileUtil.getMsg(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.prependParenthesis");
    final String appendParenthesis = PropertyFileUtil.getMsg(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.appendParenthesis");
    final String separator = PropertyFileUtil.getMsg(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.separator");

    boolean is1stTime = true;
    for (String itemName : ObjectsUtil.paramRequireNonNull(itemNames)) {

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

  private @Nonnull ModelAndView appExceptionFinalHandler(@Nonnull SplibGeneralController<?> ctrl,
      @Nullable UserDetails loginUser, boolean isRedirect, @Nullable ReturnUrlBean redirectBean) {

    SplibGeneralForm[] forms =
        (SplibGeneralForm[]) getModel().getAttribute(SplibWebConstants.KEY_FORMS);

    // #603:
    // 本当は、ExceptionHandlerのエラー処理の中でも、redirectしない場合のみprepareFormを呼べばいいはずなのだが
    // そこのhandlingはまだできていない。必要時に整理。
    getController().getService().prepareForm(Arrays.asList(forms), loginUser);

    // redirectBean
    // redirectBeanがない場合は自画面遷移。この場合は他のmodelの情報も全て遷移先に渡す。
    // 後続処理の簡便化のため、自画面遷移の場合のredirectBeanを生成しておく。
    if (redirectBean == null) {
      redirectBean = new ReturnUrlBean(ctrl, util, false);
    }

    // redirectの有無で分岐
    if (!isRedirect) {
      return new ModelAndView(ctrl.getDefaultHtmlPageName(), getModel().asMap());

    } else {
      // redirectBean == nullの場合は自画面遷移、自画面遷移の場合はmodelの情報も保持する
      String path = util.prepareForPageTransition(request, ctrl, redirectBean, getModel(), true);

      return new ModelAndView(path);
    }
  }

  /**
   * Catches {@code AppWarningException}.
   * 
   * @param exception AppWarningException
   * @param loginUser UserDetails, may be {@code null} when the user is not logged in
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({AppWarningException.class})
  public @Nonnull ModelAndView handleAppWarningException(@Nonnull WebAppWarningException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    MessagesBean requestResult =
        ((MessagesBean) getModel().getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));

    Objects.requireNonNull(requestResult);
    requestResult.setWarnMessage(
        exception.getMessageId(), PropertyFileUtil.getMsg(request.getLocale(),
            exception.getMessageId(), exception.getMessageArgs()),
        exception.buttonIdToPressOnConfirm());

    // warningはsubmitが完了していないことから同じ画面に戻る処理なので、別ページへのredirectは発生しない
    // PRGのためisRedirectはtrueとしておく
    return appExceptionFinalHandler(getController(), loginUser, false, null);
  }

  /**
   * Catches {@code AppException}.
   * 
   * @param exception AppException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({AppException.class})
  public @Nonnull ModelAndView handleAppException(@Nonnull AppException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    ReturnUrlBean redirectBean = null;
    MessagesBean requestResult =
        ((MessagesBean) getModel().getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));

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

    } else if (exception instanceof FormInputValidationException) {
      List<BeanValidationAppException> list =
          getBeanValidationAppExceptionList((FormInputValidationException) exception, loginUser);
      exList.addAll(list);

    } else {
      exList.add((SingleAppException) exception);
    }

    // exList内のexceptionを一つずつ処理
    for (SingleAppException saex : exList) {

      // format of each itemName: "record.field" or "record.childRecord[.grandChildRecord...].field"
      String[] itemNames = null;
      // SplibBaseWithRecordController の子でない場合は、そもそもrecord, fieldの定義がlibrary内で確立されていないので処理対象外とする。
      if (saex instanceof BizLogicAppException) {
        BizLogicAppException ex = (BizLogicAppException) saex;
        itemNames =
            (ex.getErrorFields() == null) ? new String[] {} : ex.getErrorFields().getFields();

        // itemNameは、html側としてはrootRecordNameから始まる必要があるが、java側からすると毎回書くのは面倒なので、
        // field名のみを記載することを推奨とする。
        // その場合、htmlと整合を取るためにここでrootRecordNameを追加しておく
        List<String> modifiedItemNameList = new ArrayList<>();
        String prefix = getController().getRootRecordName() + ".";
        for (String field : itemNames) {
          modifiedItemNameList.add(field.startsWith(prefix) ? field : (prefix + field));
        }

        itemNames = modifiedItemNameList.toArray(new String[modifiedItemNameList.size()]);

        if (ex instanceof BizLogicRedirectAppException) {
          String redirectPath = ((BizLogicRedirectAppException) ex).getRedirectPath();
          redirectBean = new ReturnUrlBean(getController(), redirectPath);
        }

      } else if (saex instanceof BeanValidationAppException) {
        BeanValidationErrorInfoBean bean =
            ((BeanValidationAppException) saex).getBeanValidationErrorInfoBean();
        // String itemName = cv.getPropertyPath().toString();
        // String recordName = itemName.substring(0, itemName.indexOf("."));
        itemNames = new String[] {bean.getItemName()};
      }

      String message =
          new ExceptionUtil().getAppExceptionMessageList(saex, request.getLocale()).get(0);

      String str = null;
      
      List<String> labelItemNameList = new ArrayList<>();
      // messageは既にmessage.propertiesのメッセージを取得し、パラメータも埋めた状態だが、
      // それでも{0}が残っている場合はfieldsの値を元に項目名を埋める。
      for (String itemName : itemNames) {
        labelItemNameList.add(getHtmlItem(itemName).getLabelItemName());
      }

      String[] labelItemNames = labelItemNameList.toArray(new String[labelItemNameList.size()]);
      message = addFieldNames(message, labelItemNames);

      requestResult.setErrorMessage(message, itemNames);
    }

    redirectBean =
        redirectBean == null ? getController().getRedirectUrlOnAppExceptionBean() : redirectBean;
    return appExceptionFinalHandler(getController(), loginUser, true, redirectBean);
  }

  private HtmlItem getHtmlItem(String itemName) {
    // 複数のformへの対応は別途実施
    SplibGeneralForm form =
        ((SplibGeneralForm[]) getModel().getAttribute(SplibWebConstants.KEY_FORMS))[0];

    String rootRecordName = getController().getRootRecordName();

    HtmlItem htmlItem = null;
    for (HtmlItem item : ((RecordInterface) form.getRootRecord(rootRecordName)).getHtmlItems()) {
      if (itemName.equals(rootRecordName + "." + item.getItemName())) {
        htmlItem = item;
        break;
      }
    }

    return htmlItem;
  }

  private List<BeanValidationAppException> getBeanValidationAppExceptionList(
      FormInputValidationException exception, UserDetails loginUser) {

    // spring mvcでのvalidation結果からは情報がうまく取れないので、改めてvalidationを行う
    List<BeanValidationErrorInfoBean> errorList = new ArrayList<>();

    for (ConstraintViolation<?> cv : new BeanValidationUtil().validate(exception.getForm(),
        request.getLocale())) {
      String itemId = cv.getPropertyPath().toString();
      String itemName = itemId;
      String recordName = itemName.substring(0, itemName.indexOf("."));
      itemName = ((RecordInterface) exception.getForm().getRootRecord(recordName))
          .getLabelItemName(recordName, itemName);

      errorList.add(new BeanValidationErrorInfoBean(cv));
    }

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

    for (BeanValidationErrorInfoBean bean : errorList) {
      // String message = bean.getMessage();
      String itemId = bean.getPropertyPath().toString();

      String itemName = itemId;
      String recordName = itemName.substring(0, itemName.indexOf("."));
//      itemName = ((RecordInterface) exception.getForm().getRootRecord(recordName))
//          .getLabelItemName(recordName, itemName);
      bean.setItemName(itemName);
    }

    return errorList.stream().map(bean -> new BeanValidationAppException(bean)).toList();
  }

  /*
   * bean validation標準の各種validatorが、""の場合でもSize validatorのエラーが表示されるなどイマイチ。
   * 空欄の場合には空欄のエラーのみを出したいので、同一項目で、NotEmptyと別のvalidatorが同時に存在する場合はNotEmpty以外を間引く。
   */
  private void removeDuplicatedValidators(List<BeanValidationErrorInfoBean> cvList) {

    // 一度、field名をkey, valueをsetとしcvを複数格納するmapを作成し、そのkeyに2件以上validatorが存在しかつNotEmptyが存在する場合は
    // NotEmpty以外を間引く、というロジックとする
    Map<String, Set<BeanValidationErrorInfoBean>> duplicateCheckMap = new HashMap<>();

    // 後続処理のため、NotEmptyを保持するkeyを保持しておく
    Set<String> keySetWithNotEmpty = new HashSet<>();

    for (BeanValidationErrorInfoBean cv : cvList) {
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
      for (BeanValidationErrorInfoBean cv : duplicateCheckMap.get(key)) {
        if (!isNotEmptyValidator(cv)) {
          cvList.remove(cv);
        }
      }
    }
  }

  private boolean isNotEmptyValidator(BeanValidationErrorInfoBean cv) {
    String validatorClass = cv.getValidatorClass();
    return validatorClass.endsWith("NotEmpty") || validatorClass.endsWith("NotEmptyIfValid");
  }

  private Comparator<BeanValidationErrorInfoBean> getComparator() {
    return new Comparator<>() {
      @Override
      public int compare(BeanValidationErrorInfoBean f1, BeanValidationErrorInfoBean f2) {
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

  /**
   * Catches {@code OverlappingFileLockException}.
   * 
   * @param exception AppException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({OverlappingFileLockException.class})
  public @Nonnull ModelAndView handleOptimisticLockingFailureException(
      @Nonnull OverlappingFileLockException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    // 通常のチェックエラー扱いとする
    return handleAppException(
        new BizLogicAppException("jp.ecuacion.splib.web.common.message.optimisticLocking"),
        loginUser);
  }

  /**
   * Catches {@code HttpRequestMethodNotSupportedException}, which means HTTP response 403.
   * 
   * @param exception HttpRequestMethodNotSupportedException
   * @return redirect URL
   */
  @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
  public @Nonnull String handleHttpRequestMethodNotSupportedException(
      @Nonnull HttpRequestMethodNotSupportedException exception) {

    logUtil.logError(exception, request.getLocale());
    actionOnThrowable.execute(exception);

    return "redirect:/" + PropertyFileUtil.getApp("jp.ecuacion.splib.web.system-error.go-to-path");
  }

  /**
   * Catches {@code NoResourceFoundException}, which means HTTP response 404.
   * 
   * <p>This throws NoResourceFoundException again, that means this method do nothing. 
   *     It's used just to debug 404 occurrence procedure.</p>
   * 
   * @param exception NoResourceFoundException
   * @throws NoResourceFoundException NoResourceFoundException
   */
  @ExceptionHandler({NoResourceFoundException.class})
  public void handleNoResourceFoundException(@Nonnull NoResourceFoundException exception)
      throws NoResourceFoundException {
    // 後述のhandleThrowableに含まれないよう別出ししているが、特に処理せずそのまま投げることで
    // spring標準のエラー処理（error/404.htmlを参照）の動きとする

    throw exception;
  }

  /**
   * Catches {@code HtmlFileNotFoundException}, 
   * which means {@code ShowPageController} cannot find the html file specified 
   * to the parameter of the url.
   * 
   * <p>Since users can set the url parameter, system error is not very preferable.</p>
   * 
   * @param exception HtmlFileNotFoundException
   * @return ModelAndView
   */
  @ExceptionHandler({HtmlFileNotFoundException.class})
  public @Nonnull ModelAndView handleHtmlFileNotFoundException(
      @Nonnull HtmlFileNotFoundException exception) {

    detailLog.info("Designated html file not found. html file name = " + exception.getFileName());

    // それっぽいURLにredirectしておく。なければさらにredirectされて適切な画面が表示される。
    return new ModelAndView("redirect:/public/home/page");
  }

  /**
   * Catches {@code HtmlFileNotAllowedToOpenException}, 
   * which means {@code ShowPageController} notices the html file doesn't have the needed option 
   * at the html tag.
   * 
   * @param exception HtmlFileNotAllowedToOpenException
   * @return ModelAndView
   */
  @ExceptionHandler({HtmlFileNotAllowedToOpenException.class})
  public @Nonnull ModelAndView handleHtmlFileNotAllowedToOpenException(
      @Nonnull HtmlFileNotAllowedToOpenException exception) {

    detailLog.info("Designated html file not allowed to open. Needs to add option to html tag."
        + " html file name = " + exception.getFileName());

    // それっぽいURLにredirectしておく。なければさらにredirectされて適切な画面が表示される。
    return new ModelAndView("redirect:/public/home/page");
  }

  /**
   * Catches {@code Throwable}.
   * 
   * @param exception Throwable
   * @param model model
   * @return ModelAndView
   */
  @ExceptionHandler({Throwable.class})
  public @Nonnull ModelAndView handleThrowable(@Nonnull Throwable exception, @Nonnull Model model) {

    logUtil.logError(exception, request.getLocale());

    // 個別appの処理。mail送信など。
    actionOnThrowable.execute(exception);

    Model mdl = getModel() == null ? model : getModel();
    modelAttr.addAllToModel(mdl);

    return new ModelAndView("error", mdl.asMap(), HttpStatusCode.valueOf(500));
  }
}
