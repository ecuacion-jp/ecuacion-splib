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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.splib.web.bean.RedirectUrlBean;
import jp.ecuacion.splib.web.bean.RedirectUrlPageBean;
import jp.ecuacion.splib.web.bean.RedirectUrlPageOnSuccessBean;
import jp.ecuacion.splib.web.bean.RedirectUrlPathBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.exception.FormInputValidationException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.service.SplibGeneral1FormService;
import jp.ecuacion.splib.web.service.SplibGeneral2FormsService;
import jp.ecuacion.splib.web.service.SplibGeneralService;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import jp.ecuacion.splib.web.util.SplibUtil;
import jp.ecuacion.splib.web.util.internal.TransactionTokenUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Provides abstract general controller.
 * 
 * @param <S> SplibGeneralService
 */
public abstract class SplibGeneralController<S extends SplibGeneralService>
    extends SplibBaseController {

  /**
   * Stores controller properties.
   * 
   * <p>Since properties are many, 
   *     the properties are passed not by the constructor, but by method chains.</p>
   */
  public static class ControllerContext {

    /** 
     * Constructs a new instance.
     * 
     * <p>It's not public because it's supposed 
     * to use {@code newContext()} to get the new instance.</p>
     */
    ControllerContext() {

    }

    /** 
     * See function().
     */
    @Nonnull
    private String function;

    /** 
     * See subFunction().
     */
    @Nonnull
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
    private String rootRecordName;

    /**
     * Stores {@code function} and returns {@code ControllerContext}.
     * 
     * <p>See {@link ControllerContext#function()}.</p>
     * 
     * @param function function
     * @return ControllerContext
     */
    @Nonnull
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
    @Nonnull
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
    @Nonnull
    public ControllerContext subFunction(String subFunction) {
      this.subFunction = subFunction;
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
    @Nonnull
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
    @Nonnull
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
     *     In that case, specify postfix with htmlFilePostfix.</p>
     * 
     * <p>It may be {@code null}. In that case {@link ControllerContext#getDefaultHtmlFileName()}
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
     * <p>See {@link ControllerContext#rootRecordName()}.</p>
     * 
     * @param rootRecordName rootRecordName
     * @return ControllerContext
     */
    @Nonnull
    public ControllerContext rootRecordName(String rootRecordName) {
      this.rootRecordName = rootRecordName;
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
    @Nonnull
    public String rootRecordName() {
      return rootRecordName == null ? function : rootRecordName;
    }
  }

  /**
   * 
   */
  public static class PrepareSettings {
    List<String[]> paramList = new ArrayList<>();
    boolean isRedirect = true;

    /** GetMappingなどのparamsで使用する、parameterのkeyのみを使用する場合に使用。 */
    public PrepareSettings addParam(String key) {
      paramList.add(new String[] {key, ""});
      return this;
    }

    public PrepareSettings addParam(String key, String value) {
      paramList.add(new String[] {key, value});
      return this;
    }

    public List<String[]> getParamList() {
      return paramList;
    }

    public PrepareSettings noRedirect() {
      isRedirect = false;
      return this;
    }

    public boolean isRedirect() {
      return isRedirect;
    }
  }

  /**
   * Stores {@code ControllerContext}.
   */
  protected ControllerContext context;

  /**
   * Constructs a new instance and returns it.
   * 
   * @return ControllerContext
   */
  public static ControllerContext newContext() {
    return new ControllerContext();
  }

  /**
   * Stores {@code PrepareSettings}.
   */
  protected PrepareSettings prepareSettings;

  public PrepareSettings getPrepareSettings() {
    return prepareSettings;
  }

  protected RedirectUrlBean redirectUrlOnAppExceptionBean;

  protected RolesAndAuthoritiesBean rolesAndAuthoritiesBean;

  /**
   * Stores a list of services.
   * 
   * <p>If you simply @Autowire the service, you will get an error 
   *     that there are multiple injection targets 
   *     when using ConfigController or LoginController.<br>
   *     The service used is {@code SplibGeneral1FormDoNothingService}, 
   *     but the error is that there are two injection targets: 
   *     this and SplibGeneral2FormsDoNothingService.<br>
   *     The above two DoNothingServices are different classes, 
   *     so it seems that the right service can be selected but cannot.<br>
   *     Generics parameters have been assigned to those services, 
   *     so it seems that the judgment cannot be made correctly. <br>
   *     It may be a problem with spring. 
   *     As a workaround, 
   *     we will first receive it as a List and select it on the getService() side.</p>
   */
  @Autowired
  protected List<S> serviceList;

  /**
   * Returns a service instance.
   * 
   * <p>See {@code ControllerContext.serviceList}.</p>
   * 
   * @return service
   */
  public S getService() {

    Class<?> cls = null;
    if (this instanceof SplibGeneral1FormController) {
      cls = SplibGeneral1FormService.class;

    } else if (this instanceof SplibGeneral2FormsController) {
      cls = SplibGeneral2FormsService.class;

    } else {
      cls = SplibGeneralService.class;
    }

    // closureの中ではfinalでないと使えないとエラーになったのでfinal変数に入れ替え
    final Class<?> cls2 = cls;
    List<S> list = serviceList.stream().filter(e -> cls2.isAssignableFrom(e.getClass())).toList();

    if (list.size() != 1) {
      throw new RuntimeException("Injected service not 1.");
    }

    return list.get(0);
  }

  @Autowired
  private SplibUtil util;

  /**
   * Constructs a new instance with {@code function}.
   * 
   * @param function function
   */
  public SplibGeneralController(@Nonnull String function) {
    this(function, newContext());
  }

  /**
   * Constructs a new instance with {@code function} and {@code ControllerContext}.
   * 
   * @param function function
   * @param context context
   */
  protected SplibGeneralController(@Nonnull String function, @NonNull ControllerContext context) {
    context.function(function);
    this.context = context;
  }

  public String getFunction() {
    return context.function();
  }

  public String getSubFunction() {
    return context.subFunction();
  }

  public String getRootRecordName() {
    return context.rootRecordName();
  }

  public RedirectUrlBean getRedirectUrlOnAppExceptionBean() {
    return redirectUrlOnAppExceptionBean;
  }

  /**
   * Sets params in {@code ControllerContext} to {@code Model}.
   * 
   * <p>The modelAttribute common to all processes is supposed to set in SplibControllerAdvice, 
   *     but what is required when using this controller is defined here. </p>
   * 
   * <p>By the way, since the model itself does not depend on the value held by the Controller, 
   *     it is added to the request using SplibControllerAdvice.</p>
   * 
   * @param model model
   * @param loginUser UserDetails
   */
  @ModelAttribute
  private void setParamsToModel(Model model, @AuthenticationPrincipal UserDetails loginUser) {
    model.addAttribute("function", context.function());
    model.addAttribute("rootRecordName", context.rootRecordName());

    rolesAndAuthoritiesBean =
        loginUser == null ? new SplibSecurityUtil().getRolesAndAuthoritiesBean()
            : new SplibSecurityUtil().getRolesAndAuthoritiesBean(loginUser);
    model.addAttribute("rolesAndAuthorities", rolesAndAuthoritiesBean);
  }

  /** メニューなどからURLを指定された際に表示する処理のreturnとして使用。htmlページのファイル名ルールを統一化する目的で使用。 */
  protected String getReturnStringToShowPage() {
    return getDefaultHtmlFileName();
  }

  /**
   * メニューなどからURLを指定された際に表示する処理のreturnとして使用。
   * 現時点では指定されたpageをそのまま返すだけなので意味はないのだが、今後の追加処理を見込んでmethod化しておく。 現時点では、これは1
   * htmlファイルに対しcontroller、formが複数ある場合で、 かつcontrollerにはsubFunctionがあるがhtmlファイル名にはsubFunctionがつかない、
   * という場合にやむなく使用するのみで、それ以外では使用想定はなし。
   */
  protected String getReturnStringToShowPage(String page) {
    return page;
  }

  /** 処理が終わり、最終的にredirectする場合に使用。 */
  protected String getReturnStringOnSuccess(RedirectUrlBean redirectUrlBean) {

    if (redirectUrlBean instanceof RedirectUrlPageBean) {
      return ((RedirectUrlPageBean) redirectUrlBean).getUrl(util.getLoginState(),
          context.function(), getDefaultSubFunctionOnSuccess(), getDefaultPageOnSuccess());

    } else if (redirectUrlBean instanceof RedirectUrlPathBean) {
      return ((RedirectUrlPathBean) redirectUrlBean).getUrl();

    } else {
      throw new RuntimeException("RedirectUrlBeanが想定外の値です。" + redirectUrlBean);
    }
  }

  /**
   * 処理が終わり、最終的にredirectする場合に使用。 defaultPageへのredirectを簡便にするためのメソッド。
   */
  protected String getReturnStringOnSuccess() {
    return new RedirectUrlPageOnSuccessBean().getUrl(util.getLoginState(), context.function(),
        getDefaultSubFunctionOnSuccess(), getDefaultPageOnSuccess());
  }

  /**
   * 個々のsplibUtilを直接呼び出しても良いのだが、記載を簡便化するため本クラスでメソッド化しておく。 modelは指定すれば引き継ぐ対象となる。引き継ぐ必要がない場合はnullを設定。
   */
  public String prepareForSuccessRedirectAndGetPath(Model model) {
    return prepareForRedirectOrForwardAndGetPath(new RedirectUrlPageOnSuccessBean(), model);
  }

  /**
   * 個々のsplibUtilを直接呼び出しても良いのだが、記載を簡便化するため本クラスでメソッド化しておく。 modelは指定すれば引き継ぐ対象となる。引き継ぐ必要がない場合はnullを設定。
   */
  public String prepareForSuccessRedirectAndGetPath(Model model, String subFunction, String page) {
    return prepareForRedirectOrForwardAndGetPath(
        new RedirectUrlPageOnSuccessBean(subFunction, page), model);
  }

  /**
   * 個々のsplibUtilを直接呼び出しても良いのだが、記載を簡便化するため本クラスでメソッド化しておく。 modelは指定すれば引き継ぐ対象となる。引き継ぐ必要がない場合はnullを設定。
   */
  public String prepareForForwardAndGetPath(Model model) {
    return prepareForRedirectOrForwardAndGetPath(new RedirectUrlPageBean(), model);
  }

  /**
   * 個々のsplibUtilを直接呼び出しても良いのだが、記載を簡便化するため本クラスでメソッド化しておく。 modelは指定すれば引き継ぐ対象となる。引き継ぐ必要がない場合はnullを設定。
   */
  public String prepareForForwardAndGetPath(Model model, String subFunction, String page) {
    RedirectUrlPageBean redirectBean =
        (RedirectUrlPageBean) new RedirectUrlPageBean(subFunction, page).putParam("forwarded",
            (String) null);
    return prepareForRedirectOrForwardAndGetPath(redirectBean, model);
  }

  public String prepareForRedirectOrForwardAndGetPath(RedirectUrlPageBean redirectBean,
      Model model) {
    if (redirectBean.isForward()) {
      redirectBean.putParamMap(request.getParameterMap());
    }

    return util.prepareForRedirectAndGetPath(redirectBean, model != null, this, request, model);
  }

  /**
   * validation errorなどが発生した際はredirectを通常しないので、ボタンを押した際の/.../action のパスがurlに残る。
   * その画面のactionはpostで受けているのに、そのurlをbrowserのurlバーを指定してenterしてしまう（つまりgetで送信してしまう）ことがままある。
   * システムエラーになるのはよくないので、その場合は404と同様の処理としてしまう。actionにparameterをつけないと全てここに来てしまうので注意。
   */
  @GetMapping(value = "action")
  public void throw404() throws NoResourceFoundException {
    throw new NoResourceFoundException(HttpMethod.GET, "from SplibGeneralController");
  }

  /**
   * Returns default html filename corresponding to the controller. 
   * Basically {@code <function>.html}.
   * 
   * @return html filename
   */
  public String getDefaultHtmlFileName() {
    return context.function()
        + StringUtils.capitalize(context.htmlFilenamePostfix() == null ? context.subFunction()
            : context.htmlFilenamePostfix());
  }

  /** 処理成功時redirectをする場合のredirect先subFunctionのdefault。 */
  public String getDefaultSubFunctionOnSuccess() {
    return context.subFunction();
  }

  /** 処理成功時redirectをする場合のredirect先subFunctionのdefault。 */
  public String getDefaultSubFunctionOnAppException() {
    return context.subFunction();
  }

  /** 処理成功時redirectをする場合のredirect先pageのdefault。 */
  public String getDefaultPageOnSuccess() {
    return "page";
  }

  /** 処理成功時redirectをする場合のredirect先pageのdefault。 */
  public String getDefaultPageOnAppException() {
    return "page";
  }

  public void prepare(Model model, SplibGeneralForm... forms)
      throws FormInputValidationException, AppException {
    prepare(model, null, new PrepareSettings(), forms);
  }

  public void prepare(Model model, PrepareSettings settings, SplibGeneralForm... forms)
      throws FormInputValidationException, AppException {
    prepare(model, null, settings, forms);
  }

  /**
   * 現時点でloginUserを具体的に使用しているわけではないので、loginUserの引数がないmethodを呼んでも問題ないが、 未来のため一応実装しておく。
   */
  public void prepare(Model model, UserDetails loginUser, SplibGeneralForm... forms)
      throws FormInputValidationException, AppException {
    prepare(model, loginUser, new PrepareSettings(), forms);
  }

  /**
   * エラー処理などに必要な処理を行う。 本処理は、@XxxMappingにより呼び出されるメソッド全てで呼び出す必要あり。
   * validation・BLチェック含めエラー発生なし、かつredirect、かつtransactionTokenCheck不要、の場合は厳密にはチェックは不要となる。
   * が、最低でも引数なしのメソッドは呼ぶ（=transactionTokenCheckは実施）ルールとし、transactionTokenCheckが不要の場合は別途それを設定することとする
   */
  public void prepare(Model model, UserDetails loginUser, PrepareSettings settings,
      SplibGeneralForm... forms) throws FormInputValidationException, AppException {

    // 全form共通処理
    for (SplibGeneralForm form : forms) {
      // formをmodelに追加
      model.addAttribute(form);
      // controllerContextを設定
      form.setControllerContext(context);
    }

    // formは配列でも取得できるよう、別途配列のままrequestにも格納しておく
    model.addAttribute(SplibWebConstants.KEY_FORMS, forms);

    // submitされない、毎回再設定が必要な値（selectの選択肢一覧など）を設定
    // getService().prepareForm(new ArrayList<>(Arrays.asList(forms)), loginUser);

    // エラー処理のためにmodelにcontrollerを格納
    model.addAttribute(SplibWebConstants.KEY_CONTROLLER, this);

    // prepareSettingsはcontrollerに設定
    this.prepareSettings = (settings == null) ? new PrepareSettings() : settings;

    transactionTokenCheck();

    for (SplibGeneralForm form : forms) {
      if (form.getPrepareSettings().validates()) {
        validationCheck(form, form.getPrepareSettings().bindingResult(), util.getLoginState(),
            rolesAndAuthoritiesBean);
      }
    }
  }

  /**
   * 以下を実施。 設定項目の値に従ってpulldownの選択肢を動的に変更したい場合など、敢えてvalidation checkをしたくない場面もあるので、 validation
   * checkの要否を持たせている。
   * <ul>
   * <li>transactionToken check</li>
   * <li>validation check</li>
   * </ul>
   */
  protected void transactionTokenCheck() throws BizLogicAppException {
    // forwardの場合は、forward前のrequest parameterも全て付加されてくるため、2回transactionCheckが発生しエラーとなる。
    // それを避けるため、forwardなら処理をスキップする。
    String forward = request.getParameter("forward");
    if (forward != null && forward.equals("true")) {
      return;
    }

    String tokenFromHtml =
        (String) request.getParameter(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN);

    @SuppressWarnings("unchecked")
    Set<String> tokenSet = (Set<String>) request.getSession()
        .getAttribute(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN);

    if (tokenSet != null && tokenFromHtml != null) {
      if (!tokenSet.contains(tokenFromHtml)) {

        String msgId = "jp.ecuacion.splib.web.common.message.tokenInvalidate";
        throw new BizLogicAppException(msgId);
      }

      tokenSet.remove(tokenFromHtml);
    }
  }

  private void validationCheck(SplibGeneralForm form, BindingResult result, String loginState,
      RolesAndAuthoritiesBean bean) throws FormInputValidationException {
    // input validation
    boolean hasNotEmptyError = false;
    hasNotEmptyError = form.hasNotEmptyError(loginState, bean);

    if (hasNotEmptyError || (result != null && result.hasErrors())) {
      throw new FormInputValidationException(form);
    }
  }
}
