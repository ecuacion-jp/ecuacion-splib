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
package jp.ecuacion.splib.web.advice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.internal.TransactionTokenUtil;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Provides common procedure that are supposed to be done before controllers are called.
 */
@ControllerAdvice
public class SplibControllerAdvice {

  private HttpServletRequest request;

  private SplibLoginStateUtil loginStateUtil;

  /**
   * Constructs a new instance.
   *
   * @param request request
   * @param loginStateUtil loginStateUtil
   */
  public SplibControllerAdvice(HttpServletRequest request, SplibLoginStateUtil loginStateUtil) {
    this.request = request;
    this.loginStateUtil = loginStateUtil;
  }

  /**
   * Sets objects to model and add model to request attribute.
   * 
   * <p>When the browser is redirected, 
   *     this method also restores objects stored before the redirect.</p>
   * 
   * @param model model
   */
  @ModelAttribute
  public void modelAttribute(Model model) {

    // Add model to request attribute. This is needed because when model is obtained via DI
    // in the ExceptionHandler, an empty model is returned instead of the populated one.
    request.setAttribute(SplibWebConstants.KEY_MODEL, model);

    // Restore model and messages carried over from the redirect source and add to model.
    recoverRequestParametersFromRedirectContextId(model);

    // Aggregate global errors from all BindingResults, so the page-base template can
    // display them at the top without iterating over forms.
    aggregateGlobalErrors(model);

    // transactionToken
    model.addAttribute(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN,
        new TransactionTokenUtil().issueNewToken(request));

    // Add url path. Direct use of request in Thymeleaf is no longer allowed,
    // and it is recommended to set url etc. to model in the controller.
    model.addAttribute("loginState", loginStateUtil.getLoginState());
  }

  private void aggregateGlobalErrors(Model model) {
    List<String> messages = new ArrayList<>();
    for (Map.Entry<String, Object> entry : model.asMap().entrySet()) {
      if (entry.getKey().startsWith(BindingResult.MODEL_KEY_PREFIX)
          && entry.getValue() instanceof BindingResult br) {
        for (ObjectError err : br.getGlobalErrors()) {
          if (err.getDefaultMessage() != null) {
            messages.add(err.getDefaultMessage());
          }
        }
      }
    }
    model.addAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS, messages);
  }

  private void recoverRequestParametersFromRedirectContextId(Model model) {

    // Flash attributes saved before the redirect are already merged into the model by Spring.
    // If KEY_SAVED_MODEL is present, restore the model attributes saved before the redirect.
    @SuppressWarnings("unchecked")
    Map<String, Object> savedModel =
        (Map<String, Object>) model.getAttribute(SplibWebConstants.KEY_SAVED_MODEL);

    if (savedModel != null) {
      model.addAllAttributes(savedModel);

      // Validation was already performed before the redirect, so disable re-validation.
      model.asMap().values().stream()
          .filter(SplibGeneralForm.class::isInstance)
          .map(SplibGeneralForm.class::cast)
          .forEach(SplibGeneralForm::noValidate);

      model.asMap().remove(SplibWebConstants.KEY_SAVED_MODEL);
    }
  }
}
