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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.web.bean.ReturnUrlBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
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
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;

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
     * See functionKind().
     */
    private String[] functionKinds = new String[] {};

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
    @Nonnull
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
    @Nonnull
    public String mainRootRecordName() {
      return mainRootRecordName == null ? function : mainRootRecordName;
    }
  }

  @Autowired
  protected HttpServletResponse response;

  /**
   * Adds a cookie for 'downloadButton' to know the download procedure is done.
   */
  protected void addCookieForDownloadButton() {
    Cookie cookie = new Cookie("download_status", "completed");
    cookie.setPath("/");
    response.addCookie(cookie);
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

    // Replaced with a final variable because non-final variables cannot be used inside lambdas.
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

  public String[] getFunctionKinds() {
    return context.functionKinds();
  }

  public String getFunction() {
    return context.function();
  }

  public String getSubFunction() {
    return context.subFunction();
  }

  public String getRootRecordName() {
    return context.mainRootRecordName();
  }

  /**
   *  Is set If you want to redirect when {@code AppException} occurs.
   *  
   *  <p>It may be {@code null} if you don't want to redirect.</p>
   *  
   *  <p>{@code BizLogicRedirectAppException} redirect settings is priotized over this settings.</p>
   */
  protected ReturnUrlBean redirectUrlOnAppExceptionBean;

  public ReturnUrlBean getRedirectUrlOnAppExceptionBean() {
    return redirectUrlOnAppExceptionBean;
  }

  /**
   * Returns parameter list needed to add when the response is redirect to the original page.
   * 
   * <p>It can be used with the redirect by both normal end and abnormal end 
   *     (= in the case that AppException is thrown).</p>
   */
  protected List<String[]> paramListOnRedirectToSelf = new ArrayList<>();

  /**
   * Adds html parameter with no value when the request redirects to the same page.
   * 
   * @param key key
   */
  protected void addParamToParamListOnRedirectToSelf(String key) {
    paramListOnRedirectToSelf.add(new String[] {key, ""});
  }

  /**
   * Adds html parameter when the request redirects to the same page.
   * 
   * @param key key
   */
  protected void addParamToParamListOnRedirectToSelf(String key, String value) {
    paramListOnRedirectToSelf.add(new String[] {key, value});
  }

  public List<String[]> getParamListOnRedirectToSelf() {
    return paramListOnRedirectToSelf;
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
    model.addAttribute("functionKindsPathString", context.functionKinds().length == 0 ? ""
        : (StringUtil.getSeparatedValuesString(context.functionKinds(), "/") + "/"));
    model.addAttribute("function", context.function());


    model.addAttribute("mainRootRecordName", context.mainRootRecordName());
    // The following line is planned to be removed.
    model.addAttribute("rootRecordName", context.mainRootRecordName());

    rolesAndAuthoritiesBean =
        loginUser == null ? new SplibSecurityUtil().getRolesAndAuthoritiesBean()
            : new SplibSecurityUtil().getRolesAndAuthoritiesBean(loginUser);
    model.addAttribute("rolesAndAuthorities", rolesAndAuthoritiesBean);
  }

  /**
   * Returns the redirect URL which redirects to the same page with success message.
   * 
   * <p>This is a utility method to use redirect easily 
   *     without understanding {@code ReturnUrlBean}. 
   *     It's also allowed to use {@code ReturnUrlBean} directly from apps.</p>
   *     
   * @return URL
   */
  protected String getRedirectUrlOnSuccess() {
    return new ReturnUrlBean(this, util).showSuccessMessage().getUrl();
  }

  /**
   * Returns the redirect URL which redirects to the same page 
   *     and takes over {@code model} to the transitioned page.
   * 
   * <p>This is a utility method to use redirect easily 
   *     without understanding {@code ReturnUrlBean}. 
   *     It's also allowed to use {@code ReturnUrlBean} directly from apps.</p>
   * 
   * @param model model
   * @return URL
   */
  public String redirectToSamePageTakingOverModel(Model model) {
    return redirectToSamePageTakingOverModel(model, false);
  }

  /**
   * Returns the redirect URL which redirects to the same page 
   *     and takes over {@code model} to the transitioned page.
   * 
   * <p>This is a utility method to use redirect easily 
   *     without understanding {@code ReturnUrlBean}. 
   *     It's also allowed to use {@code ReturnUrlBean} directly from apps.</p>
   * 
   * @param model model
   * @param showsSuccessMessage showsSuccessMessage
   * @return URL
   */
  public String redirectToSamePageTakingOverModel(Model model, boolean showsSuccessMessage) {
    ReturnUrlBean bean = new ReturnUrlBean(this, util);
    if (showsSuccessMessage) {
      bean.showSuccessMessage();
    }

    return util.prepareForPageTransition(request, bean, model, false);
  }

  /**
   * Returns default html filename corresponding to the controller. 
   * 
   * <p>Basically {@code <function>.html}.<br>
   *     If {@code subFunction} is set, 
   *     it becomes {@code <function> + capitalize(<subFuction>).html}.<br>
   *     If {@code htmlFilenamePostfix} is set, 
   *     it becomes  {@code <function> + capitalize(<htmlFilenamePostfix>).html}.<br>
   *     (not {@code <function> + capitalize(<subFunction>)
   *      + capitalize(<htmlFilenamePostfix>).html})</p>
   * 
   * @return html filename
   */
  public String getDefaultHtmlPageName() {
    String functionKindPath = context.functionKinds().length == 0 ? ""
        : StringUtil.getSeparatedValuesString(context.functionKinds(), "/") + "/";
    return functionKindPath + context.function()
        + StringUtils.capitalize(context.htmlFilenamePostfix() == null ? context.subFunction()
            : context.htmlFilenamePostfix());
  }

  /**
   * Provides the default {@code subFunction} value as the part of the redirecting URL
   * after the server procedure finished successfully.
   * 
   * <p>In this class {@code context.subFunction()} is set as the default return value,
   *     but for instance in "edit" controller class, "searchList" is set 
   *     because after insert or update a record, going back to the list screen 
   *     seems to be normal.</p>
   *     
   * @return default subFunction value
   */
  public String getDefaultDestSubFunctionOnNormalEnd() {
    return context.subFunction();
  }

  /**
   * Provides the default {@code subFunction} value as the part of the redirecting URL
   * after the server procedure finished with error.
   * 
   * <p>In this class {@code context.subFunction()} is set as the default return value,
   *     but for instance in "edit" controller class, "searchList" is set 
   *     because after insert or update a record, going back to the list screen 
   *     seems to be normal.</p>
   *     
   * @return default subFunction value
   */
  public String getDefaultDestSubFunctionOnAbnormalEnd() {
    return context.subFunction();
  }

  /**
   * Provides the default {@code subFunction} value as the part of the redirecting URL
   * after the server procedure finished successfully.
   * 
   * <p>In this class {@code "page"} is set as the default return value.</p>
   *     
   * @return default subFunction value
   */
  public String getDefaultDestPageOnNormalEnd() {
    return "page";
  }

  /**
   * Provides the default {@code page} value as the part of the redirecting URL
   * after the server procedure finished with error.
   * 
   * <p>In this class {@code "page"} is set as the default return value.</p>
   *     
   * @return default subFunction value
   */
  public String getDefaultDestPageOnAbnormalEnd() {
    return "page";
  }

  /**
   * Carries out the procedure that is needed 
   * after the service procedure ended or the service throws exceptions.
   * 
   * <p>All the method with {@code @XxxMapping}
   *     needs to call this method at the first line of the method.<br>
   * Strictly speaking, the check is not needed when there are no errors including validation
   * and BL checks, redirect is performed, and transactionTokenCheck is not required.
   * However, the rule is that at minimum the no-argument method must be called
   * (= transactionTokenCheck is performed), and if transactionTokenCheck is not needed,
   * that must be configured separately.
   * </p>
   * 
   * @param model model
   * @param forms forms
   * @throws AppException AppException
   */
  public void prepare(Model model, SplibGeneralForm... forms) throws AppException {
    prepare(model, null, forms);
  }

  /**
   * Carries out the procedure that is needed 
   * after the service procedure ended or the service throws exceptions.
   * Carries out the procedure that is needed for error handling and other common processing.
   * 
   * @param model model
   * @param loginUser loginUser
   * @param forms forms
   * @throws AppException AppException
   */
  public void prepare(Model model, UserDetails loginUser, SplibGeneralForm... forms)
      throws AppException {

    // Common processing for all forms.
    for (SplibGeneralForm form : forms) {
      // Add form to model.
      model.addAttribute(form);
      // Set controller context.
      form.setControllerContext(context);
    }

    // Also store forms as an array in the model so they can be retrieved as an array.
    model.addAttribute(SplibWebConstants.KEY_FORMS, forms);

    // Store controller in model for error handling.
    model.addAttribute(SplibWebConstants.KEY_CONTROLLER, this);

    // // Set prepareSettings to controller.
    // this.prepareSettings = (settings == null) ? new PrepareSettings() : settings;

    transactionTokenCheck();

    for (SplibGeneralForm form : forms) {
      if (form.getPrepareSettings().validates()) {
        validationCheck(form, form.getPrepareSettings().bindingResult(), util.getLoginState(),
            rolesAndAuthoritiesBean);
      }
    }
  }

  /**
   * Provides token check feature.
   * 
   * <p>The token check is not executed when the html page doesn't have a token.</p>
   */
  private void transactionTokenCheck() {
    // When forwarding, all request parameters from before the forward are also included,
    // causing transactionCheck to run twice and resulting in an error.
    // To avoid this, skip the process when forwarding.
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
        new Violations().add(new BusinessViolation(msgId)).throwIfAny();
      }

      tokenSet.remove(tokenFromHtml);
    }
  }

  /**
   * Provides validation check feature.
   * 
   * @param form form
   * @param result result
   * @param loginState loginState
   * @param bean bean
   * @throws FormInputValidationException FormInputValidationException
   * @throws MultipleAppException MultipleAppException
   */
  private void validationCheck(SplibGeneralForm form, BindingResult result, String loginState,
      RolesAndAuthoritiesBean bean) {
    // input validation
    Violations violations = new Violations();
    getBeanValidationAppExceptionList(violations, form, bean);
  }

  private void getBeanValidationAppExceptionList(Violations violations, SplibGeneralForm form,
      RolesAndAuthoritiesBean bean) {

    violations.addAll(Validation.buildDefaultValidatorFactory().getValidator().validate(form));

    // Add NotEmpty check results.
    Field field = form.getRootRecordFields().get(0);
    
    try {
      Object itemContainer = field.get(form);
      form.validateNotEmpty(itemContainer, violations, request.getLocale(), util.getLoginState(),
          bean);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    //
    // // Specify sort order. Item name specification may be supported in the future,
    // // but for now a fixed sort order is sufficient.
    // // violations.getBusinessViolations().stream().sorted(getComparator())
    // // .collect(Collectors.toList());
    //
    // // The standard Jakarta validation validators are suboptimal — for example,
    // // Size validator errors are shown even for empty strings.
    // // When the field is empty, only the empty-field error should be shown,
    // // so if both NotEmpty and another validator exist for the same field,
    // // remove all validators except NotEmpty.
    // // removeDuplicatedValidators(errorList);
    //
    // // This pattern (errorList.size() == 0) can occur:
    // // Spring's validation stored errors in the result,
    // // but re-running validation manually fails to detect them.
    // // Treat as a system error.
    // if (violations.getBusinessViolations().size() == 0
    // && violations.getConstraintViolations().size() == 0) {
    // String msg = """
    // Reached getBeanValidationAppExceptionList but errorList is empty.
    // This occurs when a submit button with the same name as rootRecordName is pressed.
    // (Example: when rootRecordName is "app", a button like the following causes this)
    // <div th:replace="~{bootstrap/components :: submitButton('app', ...)}"></div>
    //
    // For text input etc., in GET terms the request param appears as "app.desc=...",
    // but Spring MVC outputs the button name itself as "app=".
    // Spring may mistakenly treat "app=" as a form mapping target due to the "app" keyword,
    // attempting a mapping and throwing an error.
    // (Note: if the button name starts with "app" but differs, e.g. "appzzz",
    // this problem does not occur.)
    // """;
    // throw new RuntimeException(msg);
    // }

    violations.throwIfAny();
  }

  // /*
  // * The standard Jakarta validation validators are suboptimal — for example,
  // * Size validator errors are shown even for empty strings.
  // * When the field is empty, only the empty-field error should be shown,
  // * so if both NotEmpty and another validator exist for the same field,
  // * remove all validators except NotEmpty.
  // */
  // private void removeDuplicatedValidators(List<ConstraintViolation<?>> cvList) {
  //
  // // Build a map with field name as key and a set of CVs as value,
  // // then remove all validators except NotEmpty for keys that have 2+ validators
  // // and include NotEmpty.
  // Map<String, Set<ConstraintViolation<?>>> duplicateCheckMap = new HashMap<>();
  //
  // // Keep track of keys that hold NotEmpty for use in subsequent processing.
  // Set<String> keySetWithNotEmpty = new HashSet<>();
  //
  // for (ConstraintViolation<?> cv : cvList) {
  // String key = cv.getPropertyPath().toString();
  // if (duplicateCheckMap.get(key) == null) {
  // duplicateCheckMap.put(key, new HashSet<>());
  // }
  //
  // duplicateCheckMap.get(key).add(cv);
  //
  // // If NotEmpty, add to keySetWithNotEmpty.
  // if (isNotEmptyValidator(cv)) {
  // keySetWithNotEmpty.add(key);
  // }
  // }
  //
  // // For keys in keySetWithNotEmpty, remove all validators except NotEmpty from their value sets.
  // for (String key : keySetWithNotEmpty) {
  // for (ConstraintViolation<?> cv : duplicateCheckMap.get(key)) {
  // if (!isNotEmptyValidator(cv)) {
  // cvList.remove(cv);
  // }
  // }
  // }
  // }

  // private boolean isNotEmptyValidator(ConstraintViolation<?> cv) {
  // String validatorClass = cv instanceof ConstraintViolationBean
  // ? ((ConstraintViolationBean<?>) cv).getValidatorClass()
  // : cv.getConstraintDescriptor().getAnnotation().getClass().getSimpleName();
  // return validatorClass.endsWith("NotEmpty") || validatorClass.endsWith("NotEmptyIfValid");
  // }

  // private Comparator<ConstraintViolation<?>> getComparator() {
  // return new Comparator<>() {
  // @Override
  // public int compare(ConstraintViolation<?> f1, ConstraintViolation<?> f2) {
  // // Compare by field name.
  // int result = f1.getPropertyPath().toString().compareTo(f2.getPropertyPath().toString());
  // if (result != 0) {
  // return result;
  // }
  //
  // // If field names are the same, compare by validator name.
  // result = f1.getConstraintDescriptor().getConstraintValidatorClasses().get(0).getSimpleName()
  // .compareTo(f2.getConstraintDescriptor().getConstraintValidatorClasses().get(0)
  // .getSimpleName());
  // if (result != 0) {
  // return result;
  // }
  //
  // // If validator types are also the same, it can only be @Pattern.
  // // For Pattern, sort order is fixed by regexp, so compare by that.
  // String s1 = (String) f1.getConstraintDescriptor().getAttributes().get("regexp");
  // String s2 = (String) f2.getConstraintDescriptor().getAttributes().get("regexp");
  // return s1 == null ? -1 : s1.compareTo(s2);
  // }
  // };
  // }
}
