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
import jp.ecuacion.splib.web.exception.InputValidationException;
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

public abstract class SplibGeneralController<S extends SplibGeneralService>
    extends SplibBaseController {

  /** 通常constructorの引数とする項目だが、数が多いのでmethodChain方式で渡す形とした。 */
  public static class ControllerContext {

    /** 敢えてpublicにはしない。newSettings()から取得する形とする。 */
    ControllerContext() {

    }

    /**
     * その機能を表す名前。 form, recordなど各種クラスのprefix、urlの/xxx/searchなどの"xxx"部分などに使用。 複数の機能で同一の機能名を持たせるのは不可。
     * 
     * <p>
     * "accGroup"のようにentity名と同一にするのが推奨。（実装上機能名及び各クラスにentity名が冠されることになりわかりやすいため）
     * ただし、実際問題、同一entityを主として使用するが、複数画面用意する必要がある場合などに名前を変える必要があるため、
     * "accGroupManagement"などentity名とは異なる名前も指定可能。 主要となるEntityは同一だが用途が異なるため画面を分けたい、などの場合には本機能名を分ける。
     * 異なるControllerで同一のfunctionを指定することは不可。 本fieldの値は、html側のtemplateにも渡され、同一のパラメータ名で使用される。
     * </p>
     */
    protected String function;

    /**
     * 機能にsearch-list-editの一連の機能がある場合に、その個々の機能（edit）に対する名称。
     * nullは不可、指定なしの場合は""を設定。同一機能で複数controllerが存在する場合以外は通常は指定なしで問題なし。
     */
    protected String subFunction = "";

    /**
     * 通常はhtmlFile名はfunction +
     * subFunctionで指定されるが、1画面に複数controllerを持つ場合は、個々のcontrollerのsubFunctionとhtmlファイル名の
     * postfixは異なる場合がある。 その場合に、postfixをhtmlFilePostfixで指定する。
     */
    protected String htmlFilenamePostfix;

    /**
     * form直下のrecordのfield名。対応するentity名を使用する。 特にlist-edit
     * templateでは、form配下には一つのみのrecordを保持するルールのため、entity名と同一にしておくのが推奨。 （list-edit
     * templateではそれ以外のパターンをや流必要性がなく未実施のためサポート外）
     * 
     * <p>
     * java側では、spring側ではrecordNameが必要だが毎回書くのが面倒な際に自動補完する目的などで使用。
     * また本fieldの値は、html側のtemplateにも渡され、同一のパラメータ名で使用。
     * </p>
     * 
     * <p>
     * page-generalの場合は、recordが存在しない場合もある。その場合は""を指定。（nullは不可）
     * </p>
     */
    protected String rootRecordName;

    public ControllerContext function(String function) {
      this.function = function;
      return this;
    }

    public String function() {
      return function;
    }

    public ControllerContext subFunction(String subFunction) {
      this.subFunction = subFunction;
      return this;
    }

    public String subFunction() {
      return subFunction;
    }

    public ControllerContext htmlFilenamePostfix(String htmlFilenamePostfix) {
      this.htmlFilenamePostfix = htmlFilenamePostfix;
      return this;
    }

    public String htmlFilenamePostfix() {
      return htmlFilenamePostfix;
    }

    public ControllerContext rootRecordName(String rootRecordName) {
      this.rootRecordName = rootRecordName;
      return this;
    }

    /** 未設定の場合はfunctionと同一となる。 */
    public String rootRecordName() {
      return rootRecordName == null ? function : rootRecordName;
    }
  }

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

  protected ControllerContext context;

  public static ControllerContext newContext() {
    return new ControllerContext();
  }

  protected PrepareSettings prepareSettings;

  public PrepareSettings getPrepareSettings() {
    return prepareSettings;
  }

  protected RedirectUrlBean redirectUrlOnAppExceptionBean;

  protected RolesAndAuthoritiesBean rolesAndAuthoritiesBean;

  /**
   * 単純にserviceを@Autowiredすると、ConfigController、LoginControllerの使用時に、injection対象が複数存在する、というエラーになる。
   * 使用するserviceがSplibGeneral1FormDoNothingService
   * なのだが、injection対象がこれとSplibGeneral2FormsDoNothingServiceの 2つ存在する、というエラーなのだ。
   * 上記2つのDoNothingServiceは別クラスなので判断ついてもよさそうなのだが、状況的にはそれらのserviceではGenericsのparameterが付与されており、
   * それによって正しく判断ができていないようだ。pringの不具合かもしれない。 workaroundとして、一旦Listで受け取りgetService()側で選別する形とする。
   */
  @Autowired
  protected List<S> serviceList;

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

  /** functionを指定したconstructor。 */
  public SplibGeneralController(@Nonnull String function) {
    this(function, newContext());
  }

  /** functionを指定したconstructor。functionだけは必須なのでconstructorの引数としている。 */
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

  // // redirect元（自画面遷移）で既にmodelにformが設定されており、それが引き継がれている場合はそのformを使用、なければ新規生成。
  // protected SplibGeneralForm getForm(Model model, SplibGeneralForm form) {
  // String className = strUtil.decapitalize(form.getClass().getSimpleName());
  // return model.containsAttribute(className) ? (SplibGeneralForm) model.getAttribute(className)
  // : form;
  // }

  /**
   * 全処理に共通のmodelAttributeはSplibWebToolControllerAdviceに設定しているが、本controllerを使用する場合に必要なものはここで定義。
   * ちなみにmodel自体は、Controllerの保持する値に依存しないため、SplibControllerAdviceにてrequestに追加している。
   */
  @ModelAttribute
  private void setCommonParamsToModel(Model model, @AuthenticationPrincipal UserDetails loginUser) {
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

  /** 本controllerとペアになる画面htmlの文字列。基本は&lt;function&gt;.html。 */
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
      throws InputValidationException, AppException {
    prepare(model, null, new PrepareSettings(), forms);
  }

  public void prepare(Model model, PrepareSettings settings, SplibGeneralForm... forms)
      throws InputValidationException, AppException {
    prepare(model, null, settings, forms);
  }

  /**
   * 現時点でloginUserを具体的に使用しているわけではないので、loginUserの引数がないmethodを呼んでも問題ないが、 未来のため一応実装しておく。
   */
  public void prepare(Model model, UserDetails loginUser, SplibGeneralForm... forms)
      throws InputValidationException, AppException {
    prepare(model, loginUser, new PrepareSettings(), forms);
  }

  /**
   * エラー処理などに必要な処理を行う。 本処理は、@XxxMappingにより呼び出されるメソッド全てで呼び出す必要あり。
   * validation・BLチェック含めエラー発生なし、かつredirect、かつtransactionTokenCheck不要、の場合は厳密にはチェックは不要となる。
   * が、最低でも引数なしのメソッドは呼ぶ（=transactionTokenCheckは実施）ルールとし、transactionTokenCheckが不要の場合は別途それを設定することとする
   */
  public void prepare(Model model, UserDetails loginUser, PrepareSettings settings,
      SplibGeneralForm... forms) throws InputValidationException, AppException {

    // 全form共通処理
    for (SplibGeneralForm form : forms) {
      // formをmodelに追加
      model.addAttribute(form);
      // controllerContextを設定
      form.setControllerContext(context);
    }

    // formは配列でも取得できるよう、別途配列のままrequestにも格納しておく
    request.setAttribute(SplibWebConstants.KEY_FORMS, forms);

    // submitされない、毎回再設定が必要な値（selectの選択肢一覧など）を設定
    // getService().prepareForm(new ArrayList<>(Arrays.asList(forms)), loginUser);

    // エラー処理のためにmodelにcontrollerを格納
    request.setAttribute(SplibWebConstants.KEY_CONTROLLER, this);

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
      RolesAndAuthoritiesBean bean) throws InputValidationException {
    // input validation
    boolean hasNotEmptyError = false;
    hasNotEmptyError = form.hasNotEmptyError(loginState, bean);

    if (hasNotEmptyError || (result != null && result.hasErrors())) {
      throw new InputValidationException(form);
    }
  }
}
