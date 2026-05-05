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
package jp.ecuacion.splib.web.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validation;
import java.lang.reflect.Field;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.item.HtmlItemContainer;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import jp.ecuacion.splib.web.util.internal.SplibControllerPrepareHelper;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides a validation entry point for controllers that do not extend
 * {@code SplibGeneralController}.
 *
 * <p>Inject this bean and call one of the {@code validate} methods from any Spring MVC
 *     {@code @Controller}. On violation, {@code ViolationException} is thrown and handled
 *     by {@code SplibValidationExceptionHandler} (auto-configured) or
 *     {@code SplibExceptionHandler} (when defined by the application).</p>
 */
@Component
public class SplibValidationHelper {

  /** Request attribute key used to pass the return view name to the exception handler. */
  public static final String REQUEST_ATTR_RETURN_VIEW = "splibValidationReturnView";

  @Autowired
  private HttpServletRequest request;

  @Autowired
  private SplibControllerPrepareHelper prepareHelper;

  @Autowired
  private SplibLoginStateUtil loginStateUtil;

  /**
   * Validates {@code form} and throws {@code ViolationException} if any violations are found.
   *
   * <p>Use this overload when the form extends {@link SplibGeneralForm}.
   *     It also runs ecuacion-splib's not-empty checks via
   *     {@link SplibControllerPrepareHelper}.</p>
   *
   * @param form the form to validate
   */
  public void validate(SplibGeneralForm form) {
    form.validate(null);
    prepareHelper.validateForms(new SplibGeneralForm[] {form}, null);
  }

  /**
   * Validates {@code form} and throws {@code ViolationException} if any violations are found.
   *
   * <p>Use this overload when the form does not extend {@link SplibGeneralForm}.
   *     Runs Jakarta Validation constraints (e.g. {@code @NotEmpty}) and also
   *     {@code HtmlItemContainer.notEmpty()} checks if the form or any of its direct fields
   *     implements {@link HtmlItemContainer}.</p>
   *
   * <p>On error, the exception handler redirects to the referring page.</p>
   *
   * @param form the form to validate
   */
  public void validate(Object form) {
    Violations violations = new Violations();
    violations.addAll(Validation.buildDefaultValidatorFactory().getValidator().validate(form));
    validateHtmlItemContainers(form, violations);
    violations.throwIfAny();
  }

  /**
   * Validates {@code form} and forwards to {@code returnViewName} if any violations are found.
   *
   * <p>Use this overload when the form does not extend {@link SplibGeneralForm} and you want
   *     the exception handler to forward (not redirect) to the given view on error,
   *     which enables {@code th:errors} field-level error display.</p>
   *
   * @param form the form to validate
   * @param returnViewName the view name to forward to on validation error
   */
  public void validate(Object form, String returnViewName) {
    request.setAttribute(REQUEST_ATTR_RETURN_VIEW, returnViewName);
    validate(form);
  }

  /**
   * Runs {@code HtmlItemContainer.notEmpty()} checks against {@code form} and its direct fields.
   *
   * @param form the form object
   * @param violations violations to add errors to
   */
  private void validateHtmlItemContainers(Object form, Violations violations) {
    String loginState = loginStateUtil.getLoginState();

    if (form instanceof HtmlItemContainer container) {
      validateNotEmptyForContainer(form, container, null, loginState, violations);
    }

    for (Field field : form.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      try {
        Object fieldValue = field.get(form);
        if (fieldValue instanceof HtmlItemContainer container) {
          validateNotEmptyForContainer(form, container, field.getName(), loginState, violations);
        }
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  /**
   * Checks {@code notEmpty()} constraints on a single {@link HtmlItemContainer}.
   *
   * @param rootBean root bean (the form)
   * @param container the HtmlItemContainer to check
   * @param fieldPrefix field name prefix when the container is a nested field (may be
   *     {@code null})
   * @param loginState login state
   * @param violations violations to add errors to
   */
  private void validateNotEmptyForContainer(Object rootBean, HtmlItemContainer container,
      @Nullable String fieldPrefix, String loginState, Violations violations) {
    final String messageKey = "jakarta.validation.constraints.NotEmpty.message";
    RolesAndAuthoritiesBean bean = new SplibSecurityUtil().getRolesAndAuthoritiesBean();
    for (String path : container.getNotEmptyItemPropertyPathList(loginState, bean)) {
      Object value = container.getValue(path);
      if (value == null || (value instanceof String s && s.isEmpty())) {
        String fullPath = fieldPrefix == null ? path : fieldPrefix + "." + path;
        violations.add(new BusinessViolation(rootBean, new String[] {fullPath}, messageKey));
      }
    }
  }
}
