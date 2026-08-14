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
package jp.ecuacion.splib.web.util.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * Handles shared form-preparation steps executed on every controller request handler,
 * including binding-result registration, transaction token check, and validation.
 */
@Component
public class SplibControllerPrepareHelper {

  @Autowired
  private HttpServletRequest request;

  @Autowired
  private SplibLoginStateUtil loginStateUtil;

  /**
   * Registers a {@code BindingResult} for {@code form} in {@code model} under the
   *     conventional Spring MVC key, unless one is already present.
   *
   * @param model model
   * @param form form
   */
  public void registerBindingResult(Model model, SplibGeneralForm form) {
    String formName = StringUtils.uncapitalize(form.getClass().getSimpleName());
    String key = BindingResult.MODEL_KEY_PREFIX + formName;
    if (model.getAttribute(key) == null) {
      model.addAttribute(key, new BeanPropertyBindingResult(form, formName));
    }
  }

  /**
   * Verifies the transaction token submitted with the request.
   *
   * <p>The check is skipped when the html page does not include a token,
   *     or when the request is a server-side forward.</p>
   */
  public void transactionTokenCheck() {
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
   * Runs bean validation and not-empty checks for each form that requests validation.
   *
   * @param forms forms to validate
   * @param bean roles and authorities of the current user; may be {@code null}
   */
  public void validateForms(SplibGeneralForm[] forms, @Nullable RolesAndAuthoritiesBean bean) {
    for (SplibGeneralForm form : forms) {
      if (form.getPrepareSettings().validates()) {
        validateForm(form, bean);
      }
    }
  }

  private void validateForm(SplibGeneralForm form, @Nullable RolesAndAuthoritiesBean bean) {
    Violations violations = new Violations();
    violations.addAll(Validation.buildDefaultValidatorFactory().getValidator().validate(form));

    List<Field> rootRecordFields = form.getRootRecordFields();
    if (!rootRecordFields.isEmpty()) {
      Field field = rootRecordFields.get(0);
      try {
        Object itemContainer = Objects.requireNonNull(field.get(form));
        form.validateNotEmpty(itemContainer, violations, request.getLocale(),
            loginStateUtil.getLoginState(), bean);

      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }

    excludeConstraintViolationsMaskedByRequiredError(violations, form::toItemPropertyPath)
        .throwIfAny();
  }

  /**
   * Removes {@code ConstraintViolation}s whose {@code itemPropertyPath} already has a
   * required-field {@code BusinessViolation} (added by {@link SplibGeneralForm#validateNotEmpty}
   * or {@code SplibValidationHelper}'s own not-empty check, both of which run independently of
   * Jakarta Validation).
   *
   * <p>Without this, an empty field can show both the required-field error and an unrelated
   * constraint error (e.g. {@code @Min}/{@code @Max} fail to parse the empty value as a
   * number), which is confusing to the user.</p>
   *
   * <p>A {@code ConstraintViolation}'s {@code propertyPath} is always fully qualified from the
   * validation root (e.g. {@code "instance.defaultIntervalMinToStop"}), while whether a
   * {@code BusinessViolation}'s item property path is qualified the same way depends on the
   * caller: {@link SplibGeneralForm#validateNotEmpty} leaves it relative to the record it was
   * raised against (e.g. {@code "defaultIntervalMinToStop"}), whereas {@code
   * SplibValidationHelper} already prefixes it with the containing field name. {@code
   * toItemPropertyPath} lets each caller supply whatever conversion (if any) makes its
   * {@code ConstraintViolation} paths comparable to its own {@code BusinessViolation} paths -
   * {@link SplibGeneralForm#toItemPropertyPath} for the former, {@link Function#identity()} for
   * the latter.</p>
   *
   * @param violations violations collected so far
   * @param toItemPropertyPath converts a {@code ConstraintViolation}'s fully qualified
   *     {@code propertyPath} into the same shape as the {@code BusinessViolation} item property
   *     paths raised by the caller
   * @return a new {@code Violations} with the masked constraint violations removed
   */
  public Violations excludeConstraintViolationsMaskedByRequiredError(Violations violations,
      Function<String, String> toItemPropertyPath) {
    Set<@NonNull String> requiredItemPropertyPaths = violations.getBusinessViolations().stream()
        .filter(bv -> bv.getMessageId().equals(SplibWebConstants.MESSAGE_KEY_NOT_EMPTY))
        .flatMap(bv -> Arrays.stream(bv.getItemPropertyPaths())).collect(Collectors.toSet());

    Violations filtered = new Violations().messageParameters(violations.messageParameters());
    violations.getConstraintViolations().stream()
        .filter(cv -> !requiredItemPropertyPaths
            .contains(toItemPropertyPath.apply(cv.getPropertyPath().toString())))
        .forEach(filtered::add);
    violations.getBusinessViolations().forEach(filtered::add);
    return filtered;
  }
}
