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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.splib.web.bean.ReturnUrlBuilder;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.service.SplibGeneral1FormService;
import jp.ecuacion.splib.web.service.SplibGeneral2FormsService;
import jp.ecuacion.splib.web.service.SplibGeneralService;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.SplibSavedModelUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import jp.ecuacion.splib.web.util.internal.SplibControllerPrepareHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Provides abstract general controller.
 * 
 * @param <S> SplibGeneralService
 */
public abstract class SplibGeneralController<S extends SplibGeneralService>
    extends SplibBaseController {

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

  @Nullable
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
  private SplibLoginStateUtil loginStateUtil;

  @Autowired
  private SplibControllerPrepareHelper prepareHelper;

  /**
   * Constructs a new instance with {@code function}.
   * 
   * @param function function
   */
  public SplibGeneralController(String function) {
    this(function, newContext());
  }

  /**
   * Constructs a new instance with {@code function} and {@code ControllerContext}.
   * 
   * @param function function
   * @param context context
   */
  protected SplibGeneralController(String function, ControllerContext context) {
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
   *  Is set If you want to redirect when {@code ViolationException} occurs.
   *  
   *  <p>It may be {@code null} if you don't want to redirect.</p>
   *  
   *  <p>{@code BizLogicRedirectAppException} redirect settings is priotized over this settings.</p>
   */
  @Nullable
  protected ReturnUrlBuilder redirectUrlOnAppException;

  public @Nullable ReturnUrlBuilder getRedirectUrlOnAppException() {
    return redirectUrlOnAppException;
  }

  /**
   * Holds the parameters that should be appended to any redirect URL produced from
   * this controller (both normal end and abnormal end, including
   * {@code ViolationException}).
   *
   * <p>Picked up by {@code ReturnUrlBuilder} via
   * {@link #getParamListOnRedirect()} during construction.</p>
   */
  protected List<String[]> paramListOnRedirect = new ArrayList<>();

  /**
   * Adds an html parameter with no value to {@link #paramListOnRedirect}.
   *
   * @param key key
   */
  protected void addParamToParamListOnRedirect(String key) {
    paramListOnRedirect.add(new String[] {key, ""});
  }

  /**
   * Adds an html parameter to {@link #paramListOnRedirect}.
   *
   * @param key key
   * @param value value
   */
  protected void addParamToParamListOnRedirect(String key, String value) {
    paramListOnRedirect.add(new String[] {key, value});
  }

  /**
   * Returns {@link #paramListOnRedirect}.
   *
   * @return the parameter list
   */
  public List<String[]> getParamListOnRedirect() {
    return paramListOnRedirect;
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
  private void setParamsToModel(Model model,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) {
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
   *     without understanding {@code ReturnUrlBuilder}. 
   *     It's also allowed to use {@code ReturnUrlBuilder} directly from apps.</p>
   *     
   * @return URL
   */
  protected String getRedirectUrlOnSuccess() {
    return ReturnUrlBuilder.forNormalEnd(this, loginStateUtil).showSuccessMessage().getUrl();
  }

  /**
   * Returns the redirect URL which redirects to the same page
   *     and takes over {@code model} to the transitioned page.
   *
   * <p>This is a utility method to use redirect easily
   *     without understanding {@code ReturnUrlBuilder}.
   *     It's also allowed to use {@code ReturnUrlBuilder} directly from apps.</p>
   *
   * @param model model
   * @param redirectAttributes redirectAttributes
   * @return URL
   */
  public String redirectToSamePageTakingOverModel(Model model,
      RedirectAttributes redirectAttributes) {
    return redirectToSamePageTakingOverModel(model, false, redirectAttributes);
  }

  /**
   * Returns the redirect URL which redirects to the same page
   *     and takes over {@code model} to the transitioned page.
   *
   * <p>This is a utility method to use redirect easily
   *     without understanding {@code ReturnUrlBuilder}.
   *     It's also allowed to use {@code ReturnUrlBuilder} directly from apps.</p>
   *
   * @param model model
   * @param showsSuccessMessage showsSuccessMessage
   * @param redirectAttributes redirectAttributes
   * @return URL
   */
  public String redirectToSamePageTakingOverModel(Model model, boolean showsSuccessMessage,
      RedirectAttributes redirectAttributes) {
    ReturnUrlBuilder builder = ReturnUrlBuilder.forNormalEnd(this, loginStateUtil);
    if (showsSuccessMessage) {
      builder.showSuccessMessage();
    }

    SplibSavedModelUtil.saveToFlash(model, redirectAttributes, false);
    return builder.getUrl();
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
   */
  public void prepare(Model model, SplibGeneralForm... forms) {
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
   */
  public void prepare(Model model, @Nullable UserDetails loginUser, SplibGeneralForm... forms) {

    // Common processing for all forms.
    for (SplibGeneralForm form : forms) {
      model.addAttribute(form);
      form.setControllerContext(context);
      prepareHelper.registerBindingResult(model, form);
    }

    model.addAttribute(SplibWebConstants.KEY_FORMS, forms);
    model.addAttribute(SplibWebConstants.KEY_CONTROLLER, this);

    prepareHelper.transactionTokenCheck();
    prepareHelper.validateForms(forms, rolesAndAuthoritiesBean);
  }

}
