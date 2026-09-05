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
import jp.ecuacion.splib.web.exception.ViolationWebWarningException;
import jp.ecuacion.splib.web.exceptionhandler.internal.ViolationBindingResultMapper;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.SplibSavedModelUtil;
import jp.ecuacion.splib.web.util.internal.RefererRedirectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Provides an exception handler.
 */
public abstract class SplibWebExceptionHandler {

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

  private static final String MSG_PREFIX = "jp.ecuacion.splib.web.common.message.";

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
  protected SplibWebExceptionHandler(HttpServletRequest request,
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
   * Returns the forms registered by {@link SplibGeneralController#prepare}, or {@code null}
   * if the model is not yet available (in which case {@link #getController()} is also
   * {@code null}, since both are registered together).
   *
   * @return forms, or {@code null}
   */
  private SplibGeneralForm @Nullable [] getForms() {
    Model model = getModel();
    return model == null ? null
        : (SplibGeneralForm[]) model.getAttribute(SplibWebConstants.KEY_FORMS);
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
    SplibGeneralForm[] forms = Objects.requireNonNull(getForms());
    Objects.requireNonNull(getController()).getService().prepareForm(Arrays.asList(forms),
        loginUser);
  }

  /**
   * Redirects to {@code redirectPath} when given, otherwise back to the referring page
   * (falling back to {@code "/"}).
   *
   * <p>{@code violations}, if any, are shown on the redirect target: attached to the primary
   *     form's {@code BindingResult} when a {@link SplibGeneralController} (with forms) is
   *     present in the model, or otherwise resolved to messages and flashed under
   *     {@link SplibWebConstants#KEY_GLOBAL_ERRORS}. When attached to a {@code BindingResult},
   *     field errors are also snapshotted to flash under
   *     {@link SplibWebConstants#KEY_FLASH_FIELD_ERRORS} so they survive form re-binding on the
   *     redirect target. The current model (if any) is always saved to flash via
   *     {@link SplibSavedModelUtil#saveToFlash} so {@code SplibControllerAdvice} can restore it
   *     on the redirect target, whichever of the two targets above is used.
   *
   * @param needsMsgAtItemDefault value of {@link #PROP_KEY_SHOWN_AT_EACH_ITEM}
   * @param needsMsgAtTopDefault value of {@link #PROP_KEY_SHOWN_AT_THE_TOP}
   */
  private ModelAndView redirectWithViolations(RedirectAttributes redirectAttributes,
      @Nullable String redirectPath, Violations violations, boolean needsMsgAtItemDefault,
      boolean needsMsgAtTopDefault) {

    if (!violations.isEmpty()) {
      SplibGeneralForm[] forms = getForms();

      if (forms != null && forms.length > 0) {
        addViolationErrorsToBindingResult(new ViolationException(violations),
            getPrimaryBindingResult(), needsMsgAtItemDefault, needsMsgAtTopDefault,
            request.getLocale());

        // Save FieldErrors separately so they survive form re-binding after the redirect.
        Map<String, List<FieldError>> fieldErrorsSnapshot = snapshotFieldErrors();
        if (!fieldErrorsSnapshot.isEmpty()) {
          redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_FLASH_FIELD_ERRORS,
              fieldErrorsSnapshot);
        }
      } else {
        redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS,
            resolveMessages(violations));
      }
    }

    SplibSavedModelUtil.saveToFlash(getModel(), redirectAttributes, true);

    String redirectTarget = redirectPath;
    if (redirectTarget == null) {
      redirectTarget = "/";
      String referer = request.getHeader("Referer");
      if (referer != null) {
        try {
          redirectTarget = RefererRedirectUtil.toSameOriginRedirectTarget(referer);
        } catch (IllegalArgumentException ex) {
          LogUtil.logSystemError(detailLog, ex);
        }
      }
    }
    return new ModelAndView("redirect:" + redirectTarget);
  }

  /**
   * Same as {@link #redirectWithViolations(RedirectAttributes, String, Violations, boolean,
   * boolean)}, but always shows messages only at the top of the page (never at-item) — for
   * system-level redirects that are not tied to a specific form field.
   */
  private ModelAndView redirectWithGlobalMessage(RedirectAttributes redirectAttributes,
      @Nullable String redirectPath, Violations violations) {
    return redirectWithViolations(redirectAttributes, redirectPath, violations, false, true);
  }

  /**
   * Convenience overload of {@link #redirectWithGlobalMessage(RedirectAttributes, String,
   * Violations)} for a single message.
   */
  private ModelAndView redirectWithGlobalMessage(RedirectAttributes redirectAttributes,
      @Nullable String redirectPath, String messageId, Object... messageArgs) {
    return redirectWithGlobalMessage(redirectAttributes, redirectPath,
        new Violations().add(messageId, messageArgs));
  }

  /**
   * Convenience overload of {@link #redirectWithGlobalMessage(RedirectAttributes, String,
   * String, Object...)} that always redirects to the application's home page.
   */
  private ModelAndView redirectToHomeWithGlobalMessage(RedirectAttributes redirectAttributes,
      String messageId, Object... args) {
    return redirectWithGlobalMessage(redirectAttributes,
        new RedirectToHomePageException().getRedirectPath(), messageId, args);
  }

  /**
   * Resolves {@code violations} into a flat list of messages, without item names.
   */
  private List<String> resolveMessages(Violations violations) {
    Locale locale = request.getLocale();
    MessageParameters params = violations.messageParameters();

    List<@NonNull ConstraintViolation<?>> sortedCvs =
        ViolationBindingResultMapper.sortedConstraintViolations(violations);
    List<String> errorMessages = new ArrayList<>();
    for (ConstraintViolation<?> cv : sortedCvs) {
      errorMessages.addAll(ExceptionUtil
          .getMessageList(new Violations().messageParameters(params).add(cv), locale, false));
    }
    for (BusinessViolation bv : violations.getBusinessViolations()) {
      errorMessages.addAll(ExceptionUtil.getMessageList(new Violations().add(bv), locale, false));
    }
    return errorMessages;
  }

  /**
   * Catches {@code ViolationWarningException}.
   *
   * @param exception ViolationWarningException
   * @param loginUser UserDetails, may be {@code null} when the user is not logged in
   * @return ModelAndView
   */
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
   * <p>Dispatches depending on whether a {@link SplibGeneralController} (with forms) is
   *     registered in the model:</p>
   * <ul>
   *   <li>controller present → {@link #handleViolationExceptionWithController}</li>
   *   <li>controller absent (plain {@code SplibBaseController}) → delegates to
   *       {@link #redirectWithGlobalMessage(RedirectAttributes, String, Violations)},
   *       redirecting back to the referring page and flashing error messages without item
   *       names, since there is no form/{@code BindingResult} to attach them to.</li>
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
      return redirectWithGlobalMessage(redirectAttributes, null, exception.getViolations());
    } else {
      // SplibGeneralController — forms and BindingResults are available.
      return handleViolationExceptionWithController(exception, loginUser, redirectAttributes);
    }
  }

  /**
   * Handles {@code ViolationException} when a {@link SplibGeneralController} is present.
   *
   * <p>Delegates to {@link #redirectWithViolations} to attach violation errors to the
   *     primary {@link BindingResult}, save field-error snapshots and the full model to flash
   *     attributes, then redirects to the abnormal-end URL.</p>
   */
  private ModelAndView handleViolationExceptionWithController(ViolationException exception,
      @Nullable UserDetails loginUser, RedirectAttributes redirectAttributes) {

    boolean needsMsgAtItemDefault = Boolean
        .valueOf(PropertiesFileUtil.getApplicationOrElse(PROP_KEY_SHOWN_AT_EACH_ITEM, "false"));
    boolean needsMsgAtTopDefault = Boolean
        .valueOf(PropertiesFileUtil.getApplicationOrElse(PROP_KEY_SHOWN_AT_THE_TOP, "false"));

    prepareFormForReturn(loginUser);

    SplibGeneralController<?> controller = Objects.requireNonNull(getController());
    ReturnUrlBuilder redirectBuilder = controller.getRedirectUrlOnAppException();
    if (redirectBuilder == null) {
      redirectBuilder = ReturnUrlBuilder.forAbnormalEnd(controller, loginStateUtil);
    }

    return redirectWithViolations(redirectAttributes, redirectBuilder.getPath(),
        exception.getViolations(), needsMsgAtItemDefault, needsMsgAtTopDefault);
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
    SplibGeneralForm[] forms = getForms();
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
   * Catches {@code RedirectException}: {@code @RequestMapping} settings exist, but the
   * controller explicitly requested a redirect (e.g. the target html file does not exist).
   *
   * @param exception RedirectException
   * @param redirectAttributes RedirectAttributes
   * @return ModelAndView
   */
  @ExceptionHandler({RedirectException.class})
  public ModelAndView handleRedirectException(RedirectException exception,
      RedirectAttributes redirectAttributes) {

    Violations violations = StringUtils.isEmpty(exception.getMessageId()) ? new Violations()
        : new Violations().add(Objects.requireNonNull(exception.getMessageId()),
            (Object[]) exception.getMessageArgs());

    return redirectWithGlobalMessage(redirectAttributes, exception.getRedirectPath(),
        violations);
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
      @Nullable @AuthenticationPrincipal UserDetails loginUser,
      RedirectAttributes redirectAttributes) {

    SplibGeneralController<?> ctrl = Objects.requireNonNull(getController());

    String msgId = MSG_PREFIX + "optimisticLocking";
    if (ctrl instanceof SplibEditController) {
      String path = ReturnUrlBuilder.forNormalEnd(ctrl, loginStateUtil).getPath();
      return handleRedirectException(new RedirectException(path, msgId), redirectAttributes);
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
   *     to the referring page with a flash error message via
   *     {@link #redirectWithGlobalMessage(RedirectAttributes, String, Violations)}.</p>
   *
   * @param exception MaxUploadSizeExceededException
   * @param redirectAttributes RedirectAttributes
   * @return ModelAndView
   */
  @ExceptionHandler({MaxUploadSizeExceededException.class})
  public ModelAndView handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
      RedirectAttributes redirectAttributes) {

    return redirectWithGlobalMessage(redirectAttributes, null, new Violations()
        .add(MSG_PREFIX + "maxUploadSizeExceeded", exception.getMaxUploadSize() / (1024 * 1024)));
  }

  /**
   * Catches {@code NoResourceFoundException}: no {@code @RequestMapping} matches the request
   * URL. Redirects to the home page.
   *
   * @param exception NoResourceFoundException
   * @param redirectAttributes RedirectAttributes
   * @return ModelAndView
   */
  @ExceptionHandler({NoResourceFoundException.class})
  public ModelAndView handleNoResourceFoundException(NoResourceFoundException exception,
      RedirectAttributes redirectAttributes) {
    return redirectToHomeWithGlobalMessage(redirectAttributes,
        MSG_PREFIX + "NoResourceFoundException", exception.getResourcePath());
  }

  /**
   * Catches {@code UnsatisfiedServletRequestParameterException}. Redirects to the home page.
   *
   * @param exception UnsatisfiedServletRequestParameterException
   * @param redirectAttributes RedirectAttributes
   * @return ModelAndView
   */
  @ExceptionHandler({UnsatisfiedServletRequestParameterException.class})
  public ModelAndView handleUnsatisfiedServletRequestParameterException(
      UnsatisfiedServletRequestParameterException exception,
      RedirectAttributes redirectAttributes) {
    return redirectToHomeWithGlobalMessage(redirectAttributes,
        "jp.ecuacion.splib.web.login.message.notFound");
  }

  /**
   * Catches {@code Throwable}.
   *
   * @param exception Throwable
   * @param newModel model
   * @return ModelAndView
   */
  @ExceptionHandler({Throwable.class})
  public ModelAndView handleThrowable(Throwable exception, Model newModel) {

    LogUtil.logSystemError(detailLog, exception);

    // app dependent procedures, like sending mail.
    if (actionOnThrowable != null) {
      Objects.requireNonNull(actionOnThrowable).execute(exception);
    }

    Model mdl = getModel() == null ? newModel : getModel();
    return new ModelAndView("error", Objects.requireNonNull(mdl).asMap(),
        HttpStatusCode.valueOf(500));
  }
}
