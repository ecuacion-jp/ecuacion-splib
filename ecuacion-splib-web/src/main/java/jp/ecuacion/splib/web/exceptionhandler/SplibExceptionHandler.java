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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ConstraintViolationExceptionWithParameters;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.exception.ViolationWarningException;
import jp.ecuacion.lib.core.item.ItemContainer;
import jp.ecuacion.lib.core.jakartavalidation.constraints.ClassValidator;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.util.PropertyPathUtil;
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
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.SplibSavedModelUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
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
  public ModelAndView handleWarning(ViolationWarningException exception,
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
   * @throws Exception Exception
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

    List<ConstraintViolation<?>> sortedCvs = violations.getConstraintViolations().stream()
        .sorted(Comparator.comparing(cv -> cv.getPropertyPath().toString())).toList();
    List<String> errorMessages = new ArrayList<>();
    for (ConstraintViolation<?> cv : sortedCvs) {
      errorMessages.addAll(ExceptionUtil
          .getMessageList(new Violations().messageParameters(params).add(cv), locale, false));
    }
    for (BusinessViolation bv : violations.getBusinessViolations()) {
      errorMessages.addAll(ExceptionUtil.getMessageList(new Violations().add(bv), locale, false));
    }
    redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS, errorMessages);

    String redirectTarget = "/";
    String referer = request.getHeader("Referer");
    if (referer != null) {
      try {
        URI uri = URI.create(referer);
        redirectTarget = uri.getRawPath() != null ? uri.getRawPath() : "/";
        if (uri.getRawQuery() != null) {
          redirectTarget += "?" + uri.getRawQuery();
        }
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

    boolean needsMsgAtItemDefault = Boolean.valueOf(PropertiesFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.process-result-message.shown-at-each-item", "false"));
    boolean needsMsgAtTopDefault = Boolean.valueOf(PropertiesFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.process-result-message.shown-at-the-top", "false"));

    addViolationErrorsTo(exception, getPrimaryBindingResult(), needsMsgAtItemDefault,
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
   * <p>This method contains the core violation-to-error mapping logic, separated from
   * web-layer concerns (model, redirect, form preparation) for testability.
   * Unlike {@link #handleViolationException}, which resolves the {@code BindingResult} from
   * the Spring model, this method works with a single caller-supplied {@code BindingResult}
   * and therefore does not depend on a live HTTP request or model.</p>
   *
   * @param exception the exception whose violations should be added
   * @param br the {@code BindingResult} to populate
   * @param needsMsgAtItemDefault value of
   *     {@code jp.ecuacion.splib.web.process-result-message.shown-at-each-item}
   * @param needsMsgAtTopDefault value of
   *     {@code jp.ecuacion.splib.web.process-result-message.shown-at-the-top}
   * @param locale locale for message resolution
   * @return the same {@code BindingResult}, with errors added
   */
  @SuppressWarnings("null")
  BindingResult addViolationErrorsTo(ViolationException exception, BindingResult br,
      boolean needsMsgAtItemDefault, boolean needsMsgAtTopDefault, Locale locale) {

    validateMessageDisplayConfig(needsMsgAtItemDefault, needsMsgAtTopDefault);

    Violations violations = exception.getViolations();
    MessageParameters params = violations.messageParameters();

    List<ConstraintViolation<?>> sortedCvs = violations.getConstraintViolations().stream()
        .sorted(Comparator.comparing(cv -> cv.getPropertyPath().toString())).toList();

    boolean atEachItemErrorAdded = false;

    for (ConstraintViolation<?> cv : sortedCvs) {
      atEachItemErrorAdded |= addConstraintViolation(br, cv, params, needsMsgAtItemDefault,
          needsMsgAtTopDefault, locale);
    }
    for (BusinessViolation bv : violations.getBusinessViolations()) {
      atEachItemErrorAdded |=
          addBusinessViolation(br, bv, needsMsgAtItemDefault, needsMsgAtTopDefault, locale);
    }

    // When at least one field-level (at-each-item) error was added, prepend a summary message
    // at the top of the page. This notifies the user to scroll down and check field-level
    // messages — especially useful on tall pages where field errors may not be visible initially.
    // The summary is shown whenever a field error exists, regardless of the atTop setting.
    if (atEachItemErrorAdded) {
      String key = "jp.ecuacion.splib.web.common.message.messagesLinkedToItemsExist";
      addGlobalError(br, key, PropertiesFileUtil.getMessage(locale, key));
    }

    return br;
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
  private boolean addConstraintViolation(BindingResult br, ConstraintViolation<?> cv,
      MessageParameters params, boolean needsMsgAtItemDefault, boolean needsMsgAtTopDefault,
      Locale locale) {
    String errorCode =
        cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();

    String[] propertyPaths;
    if (isClassValidatorConstraint(cv)) {
      String beanPath = cv.getPropertyPath().toString();
      String[] annotationPaths =
          getPropertyPathsFromAnnotation(cv.getConstraintDescriptor().getAnnotation());
      // annotationPaths is guaranteed non-empty by MultiplePropertyPathsValidator.initialize(),
      // so no length check is needed here.
      if (beanPath.isEmpty()) {
        propertyPaths = qualifyItemPropertyPaths(br, annotationPaths);
      } else {
        propertyPaths =
            Arrays.stream(annotationPaths).map(p -> beanPath + "." + p).toArray(String[]::new);
      }
    } else {
      String pathStr = cv.getPropertyPath().toString();
      if (pathStr.isEmpty()) {
        // Class-level constraint (propertyPath is empty): no field to attach the error to.
        // Fall through with an empty paths array; addViolation will treat this as a
        // global (top-of-page) error via the needsMsgAtTop fallback.
        propertyPaths = new String[] {};
      } else if (br.getTarget() instanceof SplibGeneralForm form) {
        // For SplibGeneralForm targets, verify the path exists in the form records.
        // If not found, propertyPaths becomes empty and the needsMsgAtTop fallback fires
        // automatically,
        // preventing the error from being silently lost when no matching th:errors binding exists.
        String qualified = qualifyForForm(form, pathStr);
        propertyPaths = qualified != null ? new String[] {qualified} : new String[] {};
      } else {
        propertyPaths = new String[] {pathStr};
      }
    }

    Violations single = new Violations().messageParameters(params).add(cv);
    // When no property paths are resolved, there is no field to attach the error to.
    // Always fall back to a global (top-of-page) error so the message is never silently dropped.
    boolean needsMsgAtTop = needsMsgAtTopDefault || propertyPaths.length == 0;
    return addViolation(br, errorCode, propertyPaths, single, needsMsgAtItemDefault, needsMsgAtTop,
        locale);
  }

  private boolean isClassValidatorConstraint(ConstraintViolation<?> cv) {
    return cv.getConstraintDescriptor().getConstraintValidatorClasses().stream()
        .anyMatch(c -> ClassValidator.class.isAssignableFrom(c));
  }

  private String[] getPropertyPathsFromAnnotation(Annotation annotation) {
    try {
      Method method = annotation.annotationType().getMethod("propertyPath");
      Object result = method.invoke(annotation);
      if (result instanceof String[] paths) {
        return paths;
      }
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
      // annotation doesn't have propertyPath attribute
    }
    return new String[] {};
  }

  /**
   * Adds a {@code BusinessViolation} to the appropriate {@code BindingResult}.
   *
   * @return {@code true} if any at-each-item error was added.
   */
  private boolean addBusinessViolation(BindingResult br, BusinessViolation violation,
      boolean needsMsgAtItemDefault, boolean needsMsgAtTopDefault, Locale locale) {
    String errorCode = violation.getMessageId();
    Violations single = new Violations().add(violation);

    String[] inputPaths = violation.getItemPropertyPaths();
    String[] qualifiedPaths = inputPaths;
    boolean anyPathNotFound = false;

    // For SplibGeneralForm targets, verify each path exists in the form records.
    // Paths that cannot be resolved fall back to a global error so the message is never lost.
    if (br.getTarget() instanceof SplibGeneralForm form && inputPaths.length > 0) {
      List<String> foundList = new ArrayList<>();
      for (String path : inputPaths) {
        String qualified = qualifyForForm(form, path);
        if (qualified != null) {
          foundList.add(qualified);
        } else {
          anyPathNotFound = true;
        }
      }
      qualifiedPaths = foundList.toArray(new String[0]);
    }

    // Fall back to global when no paths are specified, or when any path was not found in the form.
    boolean needsMsgAtTop = needsMsgAtTopDefault || qualifiedPaths.length == 0 || anyPathNotFound;
    return addViolation(br, errorCode, qualifiedPaths, single, needsMsgAtItemDefault, needsMsgAtTop,
        locale);
  }

  /**
   * Qualifies each {@code itemPropertyPath} with the owning record's field name
   * so that it matches the path Spring binds in the {@code BindingResult}.
   *
   * <p>When the form's rootBean is itself an {@code ItemContainer}, no prefix is needed.
   *     Otherwise, the path is resolved against each record returned by
   *     {@code SplibGeneralForm#getRootRecordFields()} and the field name of the first
   *     record under which the path resolves is prepended.</p>
   */
  private String[] qualifyItemPropertyPaths(BindingResult br, String[] paths) {
    Object rootBean = br.getTarget();
    if (rootBean == null || rootBean instanceof ItemContainer) {
      return paths;
    }
    if (!(rootBean instanceof SplibGeneralForm form)) {
      return paths;
    }
    String[] result = new String[paths.length];
    for (int i = 0; i < paths.length; i++) {
      // qualifyForForm returns null when the path is not found in any record.
      // Fall back to the original path to preserve the existing behaviour for ClassValidator
      // callers.
      String qualified = qualifyForForm(form, paths[i]);
      result[i] = qualified != null ? qualified : paths[i];
    }
    return result;
  }

  /**
   * Returns the qualified path ({@code "recordField.itemPropertyPath"}) when
   * {@code itemPropertyPath} resolves to a field in one of the form's records,
   * or {@code null} when the path cannot be resolved in any record.
   *
   * <p>Callers that need the path-not-found signal should check for {@code null}.
   * Callers that want the old "return original path" behaviour should fall back to
   * {@code itemPropertyPath} when {@code null} is returned.</p>
   */
  @Nullable
  private String qualifyForForm(SplibGeneralForm form, String itemPropertyPath) {
    for (Field field : form.getRootRecordFields()) {
      field.setAccessible(true);
      try {
        Object value = field.get(form);
        if (!(value instanceof ItemContainer)) {
          continue;
        }
        try {
          PropertyPathUtil.getClass(value.getClass(), itemPropertyPath);
          return field.getName() + "." + itemPropertyPath;
        } catch (RuntimeException ignored) {
          // path doesn't fit this record; try the next one
        }
      } catch (IllegalAccessException ignored) {
        // skip inaccessible field
      }
    }
    return null; // path not found in any record
  }

  /**
   * Adds at-item / at-top messages for a single violation to the given {@code BindingResult}.
   *
   * @return {@code true} if any at-each-item error was added.
   */
  @SuppressWarnings("null")
  private boolean addViolation(BindingResult br, String errorCode, String[] propertyPaths,
      Violations singleViolation, boolean needsMsgAtItem, boolean needsMsgAtTop, Locale locale) {
    boolean atEachItemAdded = false;
    if (needsMsgAtItem && propertyPaths.length > 0) {
      String message = ExceptionUtil.getMessageList(singleViolation, locale, false).get(0);
      for (String propertyPath : propertyPaths) {
        addFieldError(br, propertyPath, errorCode, message);
      }
      atEachItemAdded = true;
    }
    if (needsMsgAtTop) {
      // When no field paths are available (class-level CV or global fallback),
      // item-name inclusion makes no sense; fall back to withItemName=false.
      boolean withItemName = propertyPaths.length > 0;
      String message = ExceptionUtil.getMessageList(singleViolation, locale, withItemName).get(0);
      addGlobalError(br, errorCode, message);
    }
    return atEachItemAdded;
  }

  /**
   * Adds a {@code FieldError} (at-each-item) to the given {@code BindingResult}.
   */
  private void addFieldError(BindingResult br, String propertyPath, String errorCode,
      String message) {
    br.addError(new FieldError(br.getObjectName(), propertyPath, null, false,
        new String[] {errorCode}, new Object[] {}, message));
  }

  /**
   * Adds a global error (at-the-top) to the given {@code BindingResult}.
   */
  private void addGlobalError(BindingResult br, String errorCode, String message) {
    br.reject(errorCode, new Object[] {}, message);
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
   * @throws Exception Exception
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
        addBusinessViolation(getPrimaryBindingResult(),
            new BusinessViolation(redirectException.getMessageId(),
                (Object[]) redirectException.getMessageArgs()),
            false, true, request.getLocale());
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
   * @throws Exception Exception
   */
  @ExceptionHandler({OverlappingFileLockException.class})
  public ModelAndView handleOptimisticLockingFailureException(
      @Nullable OverlappingFileLockException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser, Model model,
      RedirectAttributes redirectAttributes) {

    SplibGeneralController<?> ctrl = Objects.requireNonNull(getController());

    String msgId = "jp.ecuacion.splib.web.common.message.optimisticLocking";
    if (getController() instanceof SplibEditController) {
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
