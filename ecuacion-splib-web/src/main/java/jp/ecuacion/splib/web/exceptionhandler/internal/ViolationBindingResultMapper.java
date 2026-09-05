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
package jp.ecuacion.splib.web.exceptionhandler.internal;

import jakarta.validation.ConstraintViolation;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.item.ItemContainer;
import jp.ecuacion.lib.core.jakartavalidation.constraints.ClassValidator;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.util.PropertyPathUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.core.violation.Violations.MessageParameters;
import jp.ecuacion.splib.web.exceptionhandler.SplibWebExceptionHandler;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Maps a {@code ViolationException}'s {@code Violations} onto a {@code BindingResult}.
 *
 * <p>Contains the core violation-to-error mapping logic used by
 * {@link SplibWebExceptionHandler}, separated out because it works purely off its arguments
 * ({@code Violations}, {@code BindingResult}, {@code Locale}) with no dependency on a live
 * HTTP request or Spring {@code Model}, which keeps it independently testable.</p>
 */
public class ViolationBindingResultMapper {

  private ViolationBindingResultMapper() {}

  /**
   * Processes violations from the given {@code ViolationException} and adds errors to the
   * provided {@code BindingResult}.
   *
   * @param exception the exception whose violations should be added
   * @param br the {@code BindingResult} to populate
   * @param needsMsgAtItemDefault value of
   *     {@link SplibWebExceptionHandler#PROP_KEY_SHOWN_AT_EACH_ITEM}
   * @param needsMsgAtTopDefault value of {@link SplibWebExceptionHandler#PROP_KEY_SHOWN_AT_THE_TOP}
   * @param locale locale for message resolution
   * @return the same {@code BindingResult}, with errors added
   */
  @SuppressWarnings("null")
  public static BindingResult addViolationErrorsToBindingResult(ViolationException exception,
      BindingResult br, boolean needsMsgAtItemDefault, boolean needsMsgAtTopDefault,
      Locale locale) {

    validateMessageDisplayConfig(needsMsgAtItemDefault, needsMsgAtTopDefault);

    Violations violations = exception.getViolations();
    MessageParameters params = violations.messageParameters();
    // representativePropertyPath is a property of the Violations batch as a whole (via its
    // single MessageParameters), not of any individual violation, so it's resolved once here
    // and passed down as a plain value rather than threading MessageParameters into per-violation
    // methods.
    String representativePropertyPath = resolveRepresentativePropertyPath(br, params);

    List<ConstraintViolation<?>> sortedCvs = sortedConstraintViolations(violations);

    boolean atEachItemErrorAdded = false;

    for (ConstraintViolation<?> cv : sortedCvs) {
      if (addConstraintViolation(br, cv, params, representativePropertyPath, needsMsgAtItemDefault,
          needsMsgAtTopDefault, locale)) {
        atEachItemErrorAdded = true;
      }
    }
    for (BusinessViolation bv : violations.getBusinessViolations()) {
      if (addBusinessViolation(br, bv, representativePropertyPath, needsMsgAtItemDefault,
          needsMsgAtTopDefault, locale)) {
        atEachItemErrorAdded = true;
      }
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
   * Returns the constraint violations of {@code violations}, sorted by property path,
   * so that field-level errors are added to the {@code BindingResult} in a deterministic order.
   */
  public static List<@NonNull ConstraintViolation<?>> sortedConstraintViolations(
      Violations violations) {
    return violations.getConstraintViolations().stream()
        .sorted(Comparator.comparing(cv -> cv.getPropertyPath().toString())).toList();
  }

  /**
   * Throws if neither at-item nor at-top messaging is enabled.
   */
  private static void validateMessageDisplayConfig(boolean atItem, boolean atTop) {
    if (!atItem && !atTop) {
      throw new RuntimeException("One of '" + SplibWebExceptionHandler.PROP_KEY_SHOWN_AT_EACH_ITEM
          + "' or '" + SplibWebExceptionHandler.PROP_KEY_SHOWN_AT_THE_TOP + "' must be true.");
    }
  }

  /**
   * Adds a {@code ConstraintViolation} to the appropriate {@code BindingResult}.
   *
   * @return {@code true} if any at-each-item error was added.
   */
  private static boolean addConstraintViolation(BindingResult br, ConstraintViolation<?> cv,
      MessageParameters params, @Nullable String representativePropertyPath,
      boolean needsMsgAtItemDefault, boolean needsMsgAtTopDefault, Locale locale) {
    String errorCode =
        cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();

    String[] propertyPaths;
    boolean anyPathNotFound = false;

    if (isClassValidatorConstraint(cv)) {
      String beanPath = cv.getPropertyPath().toString();
      String[] annotationPaths =
          getPropertyPathsFromAnnotation(cv.getConstraintDescriptor().getAnnotation());
      // annotationPaths is guaranteed non-empty by MultiplePropertyPathsValidator.initialize(),
      // so no length check is needed here.
      // beanPath is the path (already fully qualified from the form root, e.g. "cloudService"
      // for a ClassValidator on a @Valid-cascaded nested record) at which the ClassValidator's
      // target object was found; when non-empty, beanPath + "." + path is therefore usually
      // already fully qualified too (e.g. "cloudService.awsAccessKeyId").
      String[] paths = beanPath.isEmpty() ? annotationPaths
          : Arrays.stream(annotationPaths).map(p -> beanPath + "." + p).toArray(String[]::new);

      if (br.getTarget() instanceof SplibGeneralForm form) {
        // For SplibGeneralForm targets, verify each path exists in the form records.
        // Paths not found fall back to a global error (same behaviour as non-ClassValidator).
        // resolveFormPath tries the path as-is first (it may already be fully qualified, as
        // above), falling back to qualifyForForm's record-relative resolution, so both shapes
        // are handled.
        propertyPaths = Arrays.stream(paths).map(path -> resolveFormPath(form, path))
            .filter(Objects::nonNull).toArray(String[]::new);
        anyPathNotFound = propertyPaths.length < paths.length;
      } else {
        propertyPaths = paths;
      }
    } else {
      String pathStr = cv.getPropertyPath().toString();
      if (pathStr.isEmpty()) {
        // Class-level constraint (propertyPath is empty): no field to attach the error to,
        // unless a representativePropertyPath is available to stand in for it.
        // ExceptionUtil.getMessageList cannot resolve an item name from an empty path
        // (it throws RequireNonEmptyException inside Item.<init>), so we use the
        // CV's already-interpolated message directly and register it as a global error.
        // The error is always shown at the top regardless of the needsMsgAtTop setting.
        addGlobalError(br, errorCode, cv.getMessage());
        String[] representativePaths =
            applyRepresentativePropertyPathFallback(new String[0], representativePropertyPath);
        if (representativePaths.length > 0) {
          addFieldError(br, representativePaths[0], errorCode, cv.getMessage());
        }
        return representativePaths.length > 0; // at-each-item error added iff fallback resolved
      } else if (br.getTarget() instanceof SplibGeneralForm form) {
        // A plain ConstraintViolation's propertyPath is usually already fully qualified from
        // the form root (e.g. "rec.acc.mailAddress"), because the framework's real validation
        // entry points (SplibControllerPrepareHelper#validateForm, SplibGeneralForm#validate,
        // SplibValidationHelper) all call Validation.validate() on the form itself, and @Valid
        // cascades down through it. resolveFormPath tries that first, falling back to
        // qualifyForForm's record-relative resolution for the rarer case of a path relative to
        // a single record, so both shapes are handled.
        String qualified = resolveFormPath(form, pathStr);
        propertyPaths = qualified != null ? new String[] {qualified} : new String[] {};
      } else {
        propertyPaths = new String[] {pathStr};
      }
    }

    boolean pathsWereEmpty = propertyPaths.length == 0;
    propertyPaths =
        applyRepresentativePropertyPathFallback(propertyPaths, representativePropertyPath);

    Violations single = new Violations().messageParameters(params).add(cv);
    // When no property paths were resolved, or when any path was not found in the form,
    // fall back to a global (top-of-page) error so the message is never silently dropped.
    boolean needsMsgAtTop = needsMsgAtTopDefault || pathsWereEmpty || anyPathNotFound;
    return addViolation(br, errorCode, propertyPaths, single, needsMsgAtItemDefault, needsMsgAtTop,
        locale);
  }

  private static boolean isClassValidatorConstraint(ConstraintViolation<?> cv) {
    return cv.getConstraintDescriptor().getConstraintValidatorClasses().stream()
        .anyMatch(c -> ClassValidator.class.isAssignableFrom(c));
  }

  private static String[] getPropertyPathsFromAnnotation(Annotation annotation) {
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
  public static boolean addBusinessViolation(BindingResult br, BusinessViolation violation,
      @Nullable String representativePropertyPath, boolean needsMsgAtItemDefault,
      boolean needsMsgAtTopDefault, Locale locale) {
    String errorCode = violation.getMessageId();
    Violations single = new Violations().add(violation);

    String[] inputPaths = violation.getItemPropertyPaths();
    String[] qualifiedPaths = inputPaths;
    boolean anyPathNotFound = false;

    // For SplibGeneralForm targets, verify each path exists in the form records.
    // Paths that cannot be resolved fall back to a global error so the message is never lost.
    // A BusinessViolation's itemPropertyPath is usually relative to the record it was raised
    // against (e.g. SplibGeneralForm#validateNotEmpty), but some callers pass an already
    // form-qualified path instead. resolveFormPath tries the path as-is first (verifyFormPath),
    // falling back to qualifyForForm's itemPropertyPath-relative resolution, so both shapes
    // are handled.
    if (br.getTarget() instanceof SplibGeneralForm form && inputPaths.length > 0) {
      qualifiedPaths = Arrays.stream(inputPaths).map(path -> resolveFormPath(form, path))
          .filter(Objects::nonNull).toArray(String[]::new);
      anyPathNotFound = qualifiedPaths.length < inputPaths.length;
    }

    qualifiedPaths =
        applyRepresentativePropertyPathFallback(qualifiedPaths, representativePropertyPath);

    // Fall back to global when no paths are specified, or when any path was not found in the form.
    boolean needsMsgAtTop = needsMsgAtTopDefault || inputPaths.length == 0 || anyPathNotFound;
    return addViolation(br, errorCode, qualifiedPaths, single, needsMsgAtItemDefault, needsMsgAtTop,
        locale);
  }

  /**
   * Resolves {@code params}' {@code representativePropertyPath} (if set) against {@code br}'s
   * form once per {@code Violations} batch, so every violation within the batch that has no
   * property path of its own can fall back to the same, already-resolved item (e.g. a file
   * upload field, for violations found deep inside the uploaded file's content).
   *
   * @return the resolved path, or {@code null} when none is set or it cannot be resolved.
   */
  @Nullable
  private static String resolveRepresentativePropertyPath(BindingResult br,
      MessageParameters params) {
    String representativePath = params.getRepresentativePropertyPath();
    if (representativePath == null) {
      return null;
    }

    return br.getTarget() instanceof SplibGeneralForm form
        ? resolveFormPath(form, representativePath)
        : representativePath;
  }

  /**
   * Returns {@code propertyPaths} unchanged unless it's empty, in which case it falls back to
   * {@code representativePropertyPath} (already resolved by
   * {@link #resolveRepresentativePropertyPath}) so a violation with no UI item of its own can
   * still highlight the batch's associated item.
   */
  private static String[] applyRepresentativePropertyPathFallback(String[] propertyPaths,
      @Nullable String representativePropertyPath) {
    return propertyPaths.length == 0 && representativePropertyPath != null
        ? new String[] {representativePropertyPath}
        : propertyPaths;
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
  private static String qualifyForForm(SplibGeneralForm form, String itemPropertyPath) {
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
   * Verifies that {@code propertyPath} (already fully qualified from the form root, e.g.
   * {@code "rec.acc.mailAddress"}) resolves to an actual field on {@code form}.
   *
   * <p>Unlike {@link #qualifyForForm}, this does not search for, or prepend, a record field
   *     name - the path is assumed to already include it. Returns {@code propertyPath} unchanged
   *     when it resolves, or {@code null} when it does not.</p>
   *
   * <p>Deliberately uses {@link PropertyPathUtil#getValue} (which walks the actual object
   *     graph via each intermediate value's runtime {@code getClass()}) rather than {@link
   *     PropertyPathUtil#getClass} (which walks declared field types starting from {@code
   *     form.getClass()}). The root record field (e.g. {@code SplibEditRecForm.rec}) is
   *     declared with a generic type ({@code R extends SplibRecord}), so its declared type is
   *     erased to the bound {@code SplibRecord} - {@code getClass}-based resolution would fail
   *     to find record-specific fields like {@code acc} beyond it. Since this is only called
   *     for a path that a real {@code ConstraintViolation} was just raised against, the
   *     intermediate objects (e.g. {@code rec}, {@code rec.acc}) are guaranteed non-null here
   *     (Bean Validation cannot cascade {@code @Valid} into a null reference), so reading
   *     through real values is safe.</p>
   */
  @Nullable
  private static String verifyFormPath(SplibGeneralForm form, String propertyPath) {
    try {
      PropertyPathUtil.getValue(form, propertyPath);
      return propertyPath;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  /**
   * Resolves {@code propertyPath} against {@code form}, trying the already-fully-qualified
   * interpretation first ({@link #verifyFormPath}) and falling back to the record-relative
   * interpretation ({@link #qualifyForForm}) when that fails.
   *
   * <p>Used for every path reaching a {@code SplibGeneralForm}-target {@code BindingResult} -
   * whether from a plain {@code ConstraintViolation}'s own propertyPath, or from a
   * {@code ClassValidator}'s {@code beanPath + "." + annotationPath}) - since both shapes can
   * occur depending on where in the object graph the failing constraint sits.</p>
   */
  @Nullable
  private static String resolveFormPath(SplibGeneralForm form, String propertyPath) {
    String qualified = verifyFormPath(form, propertyPath);
    return qualified != null ? qualified : qualifyForForm(form, propertyPath);
  }

  /**
   * Adds at-item / at-top messages for a single violation to the given {@code BindingResult}.
   *
   * @return {@code true} if any at-each-item error was added.
   */
  @SuppressWarnings("null")
  private static boolean addViolation(BindingResult br, String errorCode, String[] propertyPaths,
      Violations singleViolation, boolean needsMsgAtItem, boolean needsMsgAtTop, Locale locale) {
    boolean atEachItemAdded = false;
    if (propertyPaths.length > 0) {
      String message = ExceptionUtil.getMessageList(singleViolation, locale, false).get(0);
      for (String propertyPath : propertyPaths) {
        addFieldError(br, propertyPath, errorCode, message);
      }
      atEachItemAdded = needsMsgAtItem;
    }
    if (needsMsgAtTop) {
      String message = ExceptionUtil.getMessageList(singleViolation, locale, true).get(0);
      addGlobalError(br, errorCode, message);
    }
    return atEachItemAdded;
  }

  /**
   * Adds a {@code FieldError} (at-each-item) to the given {@code BindingResult}.
   */
  private static void addFieldError(BindingResult br, String propertyPath, String errorCode,
      String message) {
    br.addError(new FieldError(br.getObjectName(), propertyPath, null, false,
        new String[] {errorCode}, new Object[] {}, message));
  }

  /**
   * Adds a global error (at-the-top) to the given {@code BindingResult}.
   */
  private static void addGlobalError(BindingResult br, String errorCode, String message) {
    br.reject(errorCode, new Object[] {}, message);
  }
}
