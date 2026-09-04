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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import jp.ecuacion.splib.web.bean.ReturnUrlBuilder;
import jp.ecuacion.splib.web.bean.WarnMessageBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibEditController;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.RedirectException;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.exceptionhandler.internal.ViolationBindingResultMapper;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.SplibSavedModelUtil;
import jp.ecuacion.splib.web.util.internal.RefererRedirectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Provides an exception handler.
 */
public abstract class SplibExceptionHandler {

  /**
   * {@code application.properties} key controlling whether messages are shown next to each
   * field. See {@link #addViolationErrorsToBindingResult} for how it is used together with
   * {@link #PROP_KEY_SHOWN_AT_THE_TOP}.
   */
  public static final String PROP_KEY_SHOWN_AT_EACH_ITEM =
      "jp.ecuacion.splib.web.process-result-message.shown-at-each-item";

  /**
   * {@code application.properties} key controlling whether messages are shown at the top of
   * the page. See {@link #addViolationErrorsToBindingResult} for how it is used together with
   * {@link #PROP_KEY_SHOWN_AT_EACH_ITEM}.
   */
  public static final String PROP_KEY_SHOWN_AT_THE_TOP =
      "jp.ecuacion.splib.web.process-result-message.shown-at-the-top";

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
   * Returns the controller from which the exception throws,
   * or {@code null} if the model is not yet available.
   *
   * @return SplibGeneralController
   */
  protected @Nullable SplibGeneralController<?> getController() {
    Model model = getModel();
    return model == null ? null
        : (SplibGeneralController<?>) model.getAttribute(SplibWebConstants.KEY_CONTROLLER);
  }

  /**
   * Returns the model obtained at the controller, or {@code null} if the exception fired
   * before the controller set up the model (e.g. {@code NoResourceFoundException}).
   *
   * @return Model, or {@code null}
   */
  private @Nullable Model getModel() {
    return (Model) request.getAttribute(SplibWebConstants.KEY_MODEL);
  }

  /**
   * Returns the model, throwing {@code NullPointerException} if it is not available.
   *
   * <p>Use this in exception handlers that only fire after the controller has run
   *     (e.g. {@code ViolationException}, {@code OverlappingFileLockException}),
   *     where the model is guaranteed to be present.</p>
   */
  private Model requireModel() {
    return Objects.requireNonNull(getModel());
  }

  /**
   * Runs the form preparation that exception handlers share before returning a view.
   */
  private void prepareFormForReturn(@Nullable UserDetails loginUser) {
    // #603:
    // Ideally prepareForm should only be called when not redirecting within the ExceptionHandler,
    // but that handling is not yet implemented. To be refactored when needed.
    SplibGeneralForm[] forms =
        (SplibGeneralForm[]) requireModel().getAttribute(SplibWebConstants.KEY_FORMS);
    Objects.requireNonNull(getController()).getService().prepareForm(Arrays.asList(forms),
        loginUser);
  }

  /**
   * Catches {@code ViolationWarningException}.
   *
   * @param exception ViolationWarningException
   * @param loginUser UserDetails, may be {@code null} when the user is not logged in
   * @return ModelAndView
   */
  @SuppressWarnings("null")
  @ExceptionHandler({ViolationWarningException.class})
  public ModelAndView handleViolationWarningException(ViolationWarningException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) {

    BusinessViolation v = exception.getViolations().getBusinessViolations().get(0);
    String buttonId = exception instanceof ViolationWebWarningException vwwe
        ? vwwe.getButtonIdPressedIfConfirmed()
        : null;
    requireModel().addAttribute(SplibWebConstants.KEY_WARN_MESSAGE, new WarnMessageBean(
        v.getMessageId(),
        PropertiesFileUtil.getMessage(request.getLocale(), v.getMessageId(), v.getMessageArgs()),
        buttonId));

    // Since warning means the submit did not complete, processing returns to the same page,
    // so no redirect to a different page occurs.
    prepareFormForReturn(loginUser);
    return new ModelAndView(Objects.requireNonNull(getController()).getDefaultHtmlPageName(),
        requireModel().asMap());
  }

  /**
   * Catches {@code ViolationException}.
   *
   * <p>Dispatches to one of two private handlers depending on whether a
   *     {@link SplibGeneralController} (with forms) is registered in the model:</p>
   * <ul>
   *   <li>controller present → {@link #handleViolationExceptionWithController}</li>
   *   <li>controller absent (plain {@code SplibBaseController}) →
   *       {@link #handleViolationExceptionWithoutController}</li>
   * </ul>
   *
   * @param exception ViolationException
   * @param loginUser UserDetails
   * @return ModelAndView
   */
  @ExceptionHandler({ViolationException.class})
  public ModelAndView handleViolationException(ViolationException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser,
      RedirectAttributes redirectAttributes) {

    if (getController() == null) {
      // Plain @Controller / SplibBaseController — no forms registered in the model.
      return handleViolationExceptionWithoutController(exception, redirectAttributes);
    } else {
      // SplibGeneralController — forms and BindingResults are available.
      return handleViolationExceptionWithController(exception, loginUser, redirectAttributes);
    }
  }

  /**
   * Handles {@code ViolationException} when no {@link SplibGeneralController} is present.
   *
   * <p>Collects error messages without item names and redirects back to the referring page,
   *     passing the errors via a flash attribute so the redirect target can display them.</p>
   */
  @SuppressWarnings("null")
  private ModelAndView handleViolationExceptionWithoutController(ViolationException exception,
      RedirectAttributes redirectAttributes) {

    Violations violations = exception.getViolations();
    Locale locale = request.getLocale();
    MessageParameters params = violations.messageParameters();

    List<ConstraintViolation<?>> sortedCvs =
        ViolationBindingResultMapper.sortedConstraintViolations(violations);
    List<String> errorMessages = new ArrayList<>();
    for (ConstraintViolation<?> cv : sortedCvs) {
      errorMessages.addAll(ExceptionUtil
          .getMessageList(new Violations().messageParameters(params).add(cv), locale, false));
    }
    for (BusinessViolation bv : violations.getBusinessViolations()) {
      errorMessages.addAll(ExceptionUtil.getMessageList(new Violations().add(bv), locale, false));
    }
    return redirectToRefererWithGlobalErrors(errorMessages, redirectAttributes);
  }

  /**
   * Redirects back to the referring page (falling back to {@code "/"}), passing
   * {@code errorMessages} via a flash attribute so the redirect target can display them.
   *
   * <p>Used by exception handlers that fire before a {@link SplibGeneralController} is
   *     available (no model, no forms), so there is no {@code BindingResult} to attach
   *     field/global errors to.</p>
   */
  private ModelAndView redirectToRefererWithGlobalErrors(List<String> errorMessages,
      RedirectAttributes redirectAttributes) {

    redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS, errorMessages);

    String redirectTarget = "/";
    String referer = request.getHeader("Referer");
    if (referer != null) {
      try {
        redirectTarget = RefererRedirectUtil.toSameOriginRedirectTarget(referer);
      } catch (IllegalArgumentException ex) {
        LogUtil.logSystemError(detailLog, ex);
      }
    }
    return new ModelAndView("redirect:" + redirectTarget);
  }

  /**
   * Handles {@code ViolationException} when a {@link SplibGeneralController} is present.
   *
   * <p>Adds violation errors to the primary {@link BindingResult}, saves field-error snapshots
   *     and the full model to flash attributes, then redirects to the abnormal-end URL.</p>
   */
  private ModelAndView handleViolationExceptionWithController(ViolationException exception,
      @Nullable UserDetails loginUser, RedirectAttributes redirectAttributes) {

    Locale locale = request.getLocale();

    boolean needsMsgAtItemDefault = Boolean
        .valueOf(PropertiesFileUtil.getApplicationOrElse(PROP_KEY_SHOWN_AT_EACH_ITEM, "false"));
    boolean needsMsgAtTopDefault = Boolean
        .valueOf(PropertiesFileUtil.getApplicationOrElse(PROP_KEY_SHOWN_AT_THE_TOP, "false"));

    addViolationErrorsToBindingResult(exception, getPrimaryBindingResult(), needsMsgAtItemDefault,
        needsMsgAtTopDefault, locale);

    prepareFormForReturn(loginUser);

    SplibGeneralController<?> controller = Objects.requireNonNull(getController());
    ReturnUrlBuilder redirectBuilder = controller.getRedirectUrlOnAppException();
    if (redirectBuilder == null) {
      redirectBuilder = ReturnUrlBuilder.forAbnormalEnd(controller, loginStateUtil);
    }

    // Save FieldErrors separately so they survive form re-binding after the redirect.
    Map<String, List<FieldError>> fieldErrorsSnapshot = snapshotFieldErrors();
    if (!fieldErrorsSnapshot.isEmpty()) {
      redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_FLASH_FIELD_ERRORS,
          fieldErrorsSnapshot);
    }

    SplibSavedModelUtil.saveToFlash(requireModel(), redirectAttributes, true);
    return new ModelAndView(redirectBuilder.getUrl());
  }

  /**
   * Builds a snapshot of all field errors currently held in {@link BindingResult}s in the model.
   *
   * <p>The snapshot is keyed by the same {@code BindingResult.MODEL_KEY_PREFIX + formName} keys
   *     used in the model, so it can be restored after a redirect.</p>
   */
  private Map<String, List<FieldError>> snapshotFieldErrors() {
    Map<String, List<FieldError>> snapshot = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : requireModel().asMap().entrySet()) {
      if (entry.getKey().startsWith(BindingResult.MODEL_KEY_PREFIX)
          && entry.getValue() instanceof BindingResult br && !br.getFieldErrors().isEmpty()) {
        snapshot.put(entry.getKey(), new ArrayList<>(br.getFieldErrors()));
      }
    }
    return snapshot;
  }

  /**
   * Processes violations from the given {@code ViolationException} and adds errors to the
   * provided {@code BindingResult}.
   *
   * <p>Delegates the core violation-to-error mapping logic to
   * {@link ViolationBindingResultMapper}, which works with a single caller-supplied
   * {@code BindingResult} and therefore does not depend on a live HTTP request or model.
   * Unlike {@link #handleViolationException}, which resolves the {@code BindingResult} from
   * the Spring model, this method is kept as a thin wrapper for testability.</p>
   *
   * @param exception the exception whose violations should be added
   * @param br the {@code BindingResult} to populate
   * @param needsMsgAtItemDefault value of {@link #PROP_KEY_SHOWN_AT_EACH_ITEM}
   * @param needsMsgAtTopDefault value of {@link #PROP_KEY_SHOWN_AT_THE_TOP}
   * @param locale locale for message resolution
   * @return the same {@code BindingResult}, with errors added
   */
  BindingResult addViolationErrorsToBindingResult(ViolationException exception, BindingResult br,
      boolean needsMsgAtItemDefault, boolean needsMsgAtTopDefault, Locale locale) {
    return ViolationBindingResultMapper.addViolationErrorsToBindingResult(exception, br,
        needsMsgAtItemDefault, needsMsgAtTopDefault, locale);
  }

  /**
   * Returns the {@code BindingResult} for the first form in the model, used as the
   *     destination for global errors that are not bound to a specific form.
   */
  private BindingResult getPrimaryBindingResult() {
    SplibGeneralForm[] forms =
        (SplibGeneralForm[]) requireModel().getAttribute(SplibWebConstants.KEY_FORMS);
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
    return (BindingResult) requireModel().getAttribute(key);
  }

  /**
   * Catches {@code ConstraintViolationException}.
   *
   * @param exception ConstraintViolationException
   * @param loginUser UserDetails
   * @return ModelAndView
   */
  @ExceptionHandler({ConstraintViolationException.class})
  public ModelAndView handleConstraintViolationException(ConstraintViolationException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser,
      RedirectAttributes redirectAttributes) {
    MessageParameters params = exception instanceof ConstraintViolationExceptionWithParameters cvewp
        ? cvewp.getMessageParameters()
        : new MessageParameters();
    Violations violations =
        new Violations().addAll(exception.getConstraintViolations()).messageParameters(params);
    return handleViolationException(new ViolationException(violations), loginUser,
        redirectAttributes);
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
  @SuppressWarnings("null")
  @ExceptionHandler({NoResourceFoundException.class, RedirectException.class})
  public ModelAndView handleRedirectNeededExceptions(Exception exception, @Nullable Model newModel,
      RedirectAttributes redirectAttributes) {

    // Setup model if it's new.
    Model model = getModel();
    if (model == null) {
      model = Objects.requireNonNull(newModel);
    }

    if (!StringUtils.isEmpty(exception.getMessage())) {
      detailLog.info(exception.getMessage());
    }

    RedirectException redirectException = exception instanceof NoResourceFoundException nrfe
        ? new RedirectToHomePageException(
            "jp.ecuacion.splib.web.common.message.NoResourceFoundException", nrfe.getResourcePath())
        : (RedirectException) exception;

    // Logging
    if (redirectException.getLogLevel() != null) {
      detailLog.log(redirectException.getLogLevel(), redirectException.getLogString());
    }

    // Showing message
    if (!StringUtils.isEmpty(redirectException.getMessageId())) {
      SplibGeneralForm[] forms = getModel() != null
          ? (SplibGeneralForm[]) getModel().getAttribute(SplibWebConstants.KEY_FORMS)
          : null;
      if (forms != null && forms.length > 0) {
        ViolationBindingResultMapper.addBusinessViolation(getPrimaryBindingResult(),
            new BusinessViolation(redirectException.getMessageId(),
                (Object[]) redirectException.getMessageArgs()),
            null, false, true, request.getLocale());
      } else {
        // Controller#prepare did not run or no forms in model; no form/BindingResult is available.
        // Resolve the message and pass it via flash attribute so the redirect target can show it.
        String resolved = PropertiesFileUtil.getMessage(request.getLocale(),
            redirectException.getMessageId(), (Object[]) redirectException.getMessageArgs());
        redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS,
            List.of(resolved));
      }
    }

    // redirect
    ReturnUrlBuilder redirectBuilder = ReturnUrlBuilder.ofPath(redirectException.getRedirectPath());
    SplibSavedModelUtil.saveToFlash(model, redirectAttributes, true);
    return new ModelAndView(redirectBuilder.getUrl());
  }

  /**
   * Catches {@code OverlappingFileLockException}.
   *
   * @param exception OverlappingFileLockException
   * @param loginUser UserDetails
   * @return ModelAndView
   */
  @ExceptionHandler({OverlappingFileLockException.class})
  public ModelAndView handleOptimisticLockingFailureException(
      @Nullable OverlappingFileLockException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser, Model model,
      RedirectAttributes redirectAttributes) {

    SplibGeneralController<?> ctrl = Objects.requireNonNull(getController());

    String msgId = "jp.ecuacion.splib.web.common.message.optimisticLocking";
    if (ctrl instanceof SplibEditController) {
      String loginState = (String) requireModel().getAttribute("loginState");
      String path = "/" + loginState + "/" + ctrl.getFunction() + "/"
          + ctrl.getDefaultDestSubFunctionOnNormalEnd() + "/"
          + ctrl.getDefaultDestPageOnNormalEnd();
      return handleRedirectNeededExceptions(new RedirectException(path, msgId), model,
          redirectAttributes);
    } else {
      return handleViolationException(
          new ViolationException(new Violations().add(new BusinessViolation(msgId))), loginUser,
          redirectAttributes);
    }
  }

  /**
   * Catches {@code MaxUploadSizeExceededException}, thrown when an uploaded file exceeds
   * {@code spring.servlet.multipart.max-file-size} / {@code max-request-size}.
   *
   * <p>This fires while {@code DispatcherServlet} is parsing the multipart request, before the
   *     controller's {@code prepare()} runs, so no model/forms are available yet. Redirect back
   *     to the referring page with a flash error message, as
   *     {@link #handleViolationExceptionWithoutController} does.</p>
   *
   * @param exception MaxUploadSizeExceededException
   * @param redirectAttributes RedirectAttributes
   * @return ModelAndView
   */
  @ExceptionHandler({MaxUploadSizeExceededException.class})
  public ModelAndView handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
      RedirectAttributes redirectAttributes) {

    long maxUploadSizeMb = exception.getMaxUploadSize() / (1024 * 1024);
    String message = PropertiesFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.maxUploadSizeExceeded", maxUploadSizeMb);
    return redirectToRefererWithGlobalErrors(List.of(message), redirectAttributes);
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
}
