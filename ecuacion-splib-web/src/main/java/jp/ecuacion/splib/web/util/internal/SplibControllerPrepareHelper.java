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
import java.util.Set;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import org.apache.commons.lang3.StringUtils;
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

    Field field = form.getRootRecordFields().get(0);

    try {
      Object itemContainer = field.get(form);
      form.validateNotEmpty(itemContainer, violations, request.getLocale(),
          loginStateUtil.getLoginState(), bean);

    } catch (IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }

    violations.throwIfAny();
  }
}
