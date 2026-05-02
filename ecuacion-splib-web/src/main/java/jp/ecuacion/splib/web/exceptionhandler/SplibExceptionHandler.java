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
import jakarta.validation.ConstraintViolationException;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ConstraintViolationExceptionWithParameters;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.exception.ViolationWarningException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.core.violation.Violations.MessageParameters;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.web.bean.ReturnUrlBean;
import jp.ecuacion.splib.web.bean.WarnMessageBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibEditController;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.RedirectException;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Provides an exception handler.
 */
public abstract class SplibExceptionHandler {

  private DetailLogger detailLog = new DetailLogger(this);

  HttpServletRequest request;

  @Nullable
  SplibExceptionHandlerAction actionOnThrowable;

  private SplibLoginStateUtil loginStateUtil;

  /**
   * Constructs a new instance.
   *
   * @param request request
   * @param actionOnThrowable actionOnThrowable, may be {@code null}
   * @param loginStateUtil loginStateUtil
   */
  protected SplibExceptionHandler(HttpServletRequest request,
      @Nullable SplibExceptionHandlerAction actionOnThrowable, SplibLoginStateUtil loginStateUtil) {
    this.request = request;
    this.actionOnThrowable = actionOnThrowable;
    this.loginStateUtil = loginStateUtil;
  }

  /**
   * Returns the controller from which the exception throws.
   *
   * @return SplibGeneralController
   */
  protected SplibGeneralController<?> getController() {
    return (SplibGeneralController<?>) getModel().getAttribute(SplibWebConstants.KEY_CONTROLLER);
  }

  /**
   * Returns the model obtained at the controller.
   *
   * @return Model
   */
  private Model getModel() {
    return (Model) request.getAttribute(SplibWebConstants.KEY_MODEL);
  }

  /**
   * Runs the form preparation that exception handlers share before returning a view.
   */
  private void prepareFormForReturn(@Nullable UserDetails loginUser) {
    // #603:
    // Ideally prepareForm should only be called when not redirecting within the ExceptionHandler,
    // but that handling is not yet implemented. To be refactored when needed.
    SplibGeneralForm[] forms =
        (SplibGeneralForm[]) getModel().getAttribute(SplibWebConstants.KEY_FORMS);
    getController().getService().prepareForm(Arrays.asList(forms), loginUser);
  }

  /**
   * Catches {@code ViolationWarningException}.
   *
   * @param exception ViolationWarningException
   * @param loginUser UserDetails, may be {@code null} when the user is not logged in
   * @return ModelAndView
   * @throws Exception Exception
   */
  @SuppressWarnings("null")
  @ExceptionHandler({ViolationWarningException.class})
  public ModelAndView handleWarning(ViolationWarningException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    BusinessViolation v = exception.getViolations().getBusinessViolations().get(0);
    String buttonId = exception instanceof ViolationWebWarningException
        ? ((ViolationWebWarningException) exception).getButtonIdPressedIfConfirmed()
        : null;
    getModel().addAttribute(SplibWebConstants.KEY_WARN_MESSAGE, new WarnMessageBean(
        v.getMessageId(),
        PropertiesFileUtil.getMessage(request.getLocale(), v.getMessageId(), v.getMessageArgs()),
        buttonId));

    // Since warning means the submit did not complete, processing returns to the same page,
    // so no redirect to a different page occurs.
    prepareFormForReturn(loginUser);
    return new ModelAndView(getController().getDefaultHtmlPageName(), getModel().asMap());
  }

  /**
   * Catches {@code ViolationException}.
   *
   * @param exception ViolationException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @SuppressWarnings("null")
  @ExceptionHandler({ViolationException.class})
  public ModelAndView handleViolationException(ViolationException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser,
      RedirectAttributes redirectAttributes) throws Exception {

    boolean needsMsgAtItemDefault = Boolean.valueOf(PropertiesFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.process-result-message.shown-at-each-item", "false"));
    boolean needsMsgAtTopDefault = Boolean.valueOf(PropertiesFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.process-result-message.shown-at-the-top", "false"));
    validateMessageDisplayConfig(needsMsgAtItemDefault, needsMsgAtTopDefault);

    Violations violations = exception.getViolations();
    MessageParameters params = violations.messageParameters();
    Locale locale = request.getLocale();

    // Sort violations by propertyPath for stable display order across requests.
    List<ConstraintViolation<?>> sortedCvs = violations.getConstraintViolations().stream()
        .sorted(Comparator.comparing(cv -> cv.getPropertyPath().toString())).toList();

    boolean atEachItemErrorAdded = false;

    for (ConstraintViolation<?> cv : sortedCvs) {
      atEachItemErrorAdded |=
          addConstraintViolation(cv, params, needsMsgAtItemDefault, needsMsgAtTopDefault, locale);
    }
    for (BusinessViolation bv : violations.getBusinessViolations()) {
      atEachItemErrorAdded |=
          addBusinessViolation(bv, needsMsgAtItemDefault, needsMsgAtTopDefault, locale);
    }

    // When at-each-item errors exist, prepend a summary message at the top.
    if (atEachItemErrorAdded && needsMsgAtTopDefault) {
      String key = "jp.ecuacion.splib.web.common.message.messagesLinkedToItemsExist";
      addGlobalError(getPrimaryBindingResult(), key, PropertiesFileUtil.getMessage(locale, key));
    }

    prepareFormForReturn(loginUser);

    ReturnUrlBean redirectBean = getController().getRedirectUrlOnAppExceptionBean();
    if (redirectBean == null) {
      redirectBean = new ReturnUrlBean(getController(), loginStateUtil, false);
    }
    String path = redirectBean.applyTo(getModel(), redirectAttributes, true);
    return new ModelAndView(path);
  }

  /**
   * Catches {@code ConstraintViolationException}.
   *
   * @param exception ConstraintViolationException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({ConstraintViolationException.class})
  public ModelAndView handleConstraintViolationException(ConstraintViolationException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser,
      RedirectAttributes redirectAttributes) throws Exception {
    MessageParameters params = exception instanceof ConstraintViolationExceptionWithParameters
        ? ((ConstraintViolationExceptionWithParameters) exception).getMessageParameters()
        : new MessageParameters();
    Violations violations =
        new Violations().addAll(exception.getConstraintViolations()).messageParameters(params);
    return handleViolationException(new ViolationException(violations), loginUser,
        redirectAttributes);
  }

  /**
   * Catches {@code OverlappingFileLockException}.
   *
   * @param exception OverlappingFileLockException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({OverlappingFileLockException.class})
  public ModelAndView handleOptimisticLockingFailureException(
      @Nullable OverlappingFileLockException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser, Model model,
      RedirectAttributes redirectAttributes) throws Exception {

    String msgId = "jp.ecuacion.splib.web.common.message.optimisticLocking";
    if (getController() instanceof SplibEditController) {
      String loginState = (String) getModel().getAttribute("loginState");
      String path = "/" + loginState + "/" + getController().getFunction() + "/"
          + getController().getDefaultDestSubFunctionOnNormalEnd() + "/"
          + getController().getDefaultDestPageOnNormalEnd();
      return handleRedirectNeededExceptions(new RedirectException(path, msgId), model,
          redirectAttributes);
    } else {
      Violations v = new Violations();
      v.add(new BusinessViolation(msgId));
      return handleViolationException(new ViolationException(v), loginUser, redirectAttributes);
    }
  }

  /**
   * Catches some specific exceptions.
   *
   * <ul>
   * <li>NoResourceFoundException:
   * No @RequestMapping settings in controllers which matches the request url.</li>
   * <li>RedirectException: @RequestMapping settings
   * exists, but html file does not exist.</li>
   * </ul>
   *
   * @param exception Exception
   * @param newModel When Exception occurs before Controller#prepare called, getModel() is null.
   *     In that case, this new model can be used.
   *     This is different from the one you get at controller.
   * @return ModelAndView
   */
  @SuppressWarnings({"unused", "null"})
  @ExceptionHandler({NoResourceFoundException.class, RedirectException.class})
  public ModelAndView handleRedirectNeededExceptions(Exception exception, @Nullable Model newModel,
      RedirectAttributes redirectAttributes) {
    RedirectException redirectException = null;

    // Setup model if it's new.
    Model model = getModel();
    if (model == null) {
      model = Objects.requireNonNull(newModel);
    }

    if (!StringUtils.isEmpty(exception.getMessage())) {
      detailLog.info(exception.getMessage());
    }

    if (exception instanceof NoResourceFoundException) {
      String path = ((NoResourceFoundException) exception).getResourcePath();

      redirectException = new RedirectToHomePageException(
          "jp.ecuacion.splib.web.common.message.NoResourceFoundException", path);

    } else {
      redirectException = (RedirectException) exception;
    }

    // Logging
    if (redirectException.getLogLevel() != null) {
      detailLog.log(redirectException.getLogLevel(), redirectException.getLogString());
    }

    // Showing message
    if (!StringUtils.isEmpty(redirectException.getMessageId())) {
      addBusinessViolation(new BusinessViolation(redirectException.getMessageId(),
          redirectException.getMessageArgs()), false, true, request.getLocale());
    }

    // redirect
    ReturnUrlBean redirectBean = new ReturnUrlBean(redirectException.getRedirectPath());
    String path = redirectBean.applyTo(model, redirectAttributes, true);
    return new ModelAndView(path);
  }

  /**
   * Catches {@code Throwable}.
   *
   * @param exception Throwable
   * @param model model
   * @return ModelAndView
   */
  @SuppressWarnings("null")
  @ExceptionHandler({Throwable.class})
  public ModelAndView handleThrowable(Throwable exception, Model model) {

    LogUtil.logSystemError(detailLog, exception);

    // app dependent procedures, like sending mail.
    if (actionOnThrowable != null) {
      actionOnThrowable.execute(exception);
    }

    Model mdl = getModel() == null ? model : getModel();
    return new ModelAndView("error", mdl.asMap(), HttpStatusCode.valueOf(500));
  }

  /**
   * Throws if neither at-item nor at-top messaging is enabled.
   */
  private void validateMessageDisplayConfig(boolean atItem, boolean atTop) {
    if (!atItem && !atTop) {
      throw new RuntimeException(
          "One of 'jp.ecuacion.splib.web.process-result-message.shown-at-each-item' or "
              + "'jp.ecuacion.splib.web.process-result-message.shown-at-the-top' must be true.");
    }
  }

  /**
   * Adds a {@code ConstraintViolation} to the appropriate {@code BindingResult}.
   *
   * @return {@code true} if any at-each-item error was added.
   */
  private boolean addConstraintViolation(ConstraintViolation<?> cv, MessageParameters params,
      boolean needsMsgAtItemDefault, boolean needsMsgAtTopDefault, Locale locale) {
    boolean needsMsgAtItem =
        needsMsgAtItemDefault && !Boolean.TRUE.equals(params.isMessageWithItemName());
    BindingResult br = findBindingResultForViolation(cv);
    String errorCode =
        cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
    String[] propertyPaths = {cv.getPropertyPath().toString()};
    Violations single = new Violations().messageParameters(params).add(cv);
    return addViolation(br, errorCode, propertyPaths, single, needsMsgAtItem, needsMsgAtTopDefault,
        locale);
  }

  /**
   * Adds a {@code BusinessViolation} to the appropriate {@code BindingResult}.
   *
   * @return {@code true} if any at-each-item error was added.
   */
  @SuppressWarnings("null")
  private boolean addBusinessViolation(BusinessViolation violation, boolean needsMsgAtItemDefault,
      boolean needsMsgAtTopDefault, Locale locale) {
    boolean needsMsgAtItem = needsMsgAtItemDefault && violation.getItemPropertyPaths().length > 0;
    BindingResult br = getPrimaryBindingResult();
    String errorCode = violation.getMessageId();
    Violations single = new Violations().add(violation);
    return addViolation(br, errorCode, violation.getItemPropertyPaths(), single, needsMsgAtItem,
        needsMsgAtTopDefault, locale);
  }

  /**
   * Adds at-item / at-top messages for a single violation to the given {@code BindingResult}.
   *
   * @return {@code true} if any at-each-item error was added.
   */
  private boolean addViolation(BindingResult br, String errorCode, String[] propertyPaths,
      Violations singleViolation, boolean needsMsgAtItem, boolean needsMsgAtTop, Locale locale) {
    boolean atEachItemAdded = false;
    if (needsMsgAtItem) {
      String message = ExceptionUtil.getMessageList(singleViolation, locale, false).get(0);
      for (String propertyPath : propertyPaths) {
        addFieldError(br, propertyPath, errorCode, message);
        atEachItemAdded = true;
      }
    }
    if (needsMsgAtTop) {
      String message = ExceptionUtil.getMessageList(singleViolation, locale, true).get(0);
      addGlobalError(br, errorCode, message);
    }
    return atEachItemAdded;
  }

  /**
   * Returns the {@code BindingResult} that owns the field touched by the violation.
   *
   * <p>Falls back to the primary {@code BindingResult} when the rootBean cannot be matched.</p>
   */
  @SuppressWarnings("null")
  private BindingResult findBindingResultForViolation(ConstraintViolation<?> cv) {
    Object rootBean = cv.getRootBean();
    if (rootBean instanceof SplibGeneralForm form) {
      BindingResult br = getBindingResult(form);
      if (br != null) {
        return br;
      }
    }
    return getPrimaryBindingResult();
  }

  /**
   * Returns the {@code BindingResult} for the first form in the model, used as the
   *     destination for global errors that are not bound to a specific form.
   */
  private BindingResult getPrimaryBindingResult() {
    SplibGeneralForm[] forms =
        (SplibGeneralForm[]) getModel().getAttribute(SplibWebConstants.KEY_FORMS);
    if (forms == null || forms.length == 0) {
      throw new RuntimeException(
          "No forms registered in the model; cannot locate a BindingResult.");
    }
    BindingResult br = getBindingResult(forms[0]);
    if (br == null) {
      throw new RuntimeException(
          "BindingResult is not registered for form: " + forms[0].getClass().getName());
    }
    return br;
  }

  private @Nullable BindingResult getBindingResult(SplibGeneralForm form) {
    String formName = StringUtils.uncapitalize(form.getClass().getSimpleName());
    String key = BindingResult.MODEL_KEY_PREFIX + formName;
    return (BindingResult) getModel().getAttribute(key);
  }

  /**
   * Adds a {@code FieldError} (at-each-item) to the given {@code BindingResult}.
   */
  private void addFieldError(BindingResult br, String propertyPath, String errorCode,
      String message) {
    br.rejectValue(propertyPath, errorCode, new Object[] {}, message);
  }

  /**
   * Adds a global error (at-the-top) to the given {@code BindingResult}.
   */
  private void addGlobalError(BindingResult br, String errorCode, String message) {
    br.reject(errorCode, new Object[] {}, message);
  }
}
