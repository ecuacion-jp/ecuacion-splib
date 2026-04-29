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
import java.util.Map;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibUtil;
import jp.ecuacion.splib.web.util.internal.TransactionTokenUtil;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Provides common procedure that are supposed to be done before controllers are called.
 */
@ControllerAdvice
public class SplibControllerAdvice {

  private HttpServletRequest request;

  private SplibUtil util;

  /**
   * Constructs a new instance.
   *
   * @param request request
   * @param util util
   */
  public SplibControllerAdvice(HttpServletRequest request, SplibUtil util) {
    this.request = request;
    this.util = util;
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

    // transactionToken
    model.addAttribute(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN,
        new TransactionTokenUtil().issueNewToken(request));

    // Add url path. Direct use of request in Thymeleaf is no longer allowed,
    // and it is recommended to set url etc. to model in the controller.
    model.addAttribute("loginState", util.getLoginState());
  }

  private void recoverRequestParametersFromRedirectContextId(Model model) {

    // If messages and model are carried over from the redirect source, add them to model.
    String contextId = request.getParameter(SplibWebConstants.KEY_CONTEXT_ID);
    if (contextId != null && !contextId.isEmpty()) {

      @SuppressWarnings("unchecked")
      Map<String, Object> contextMap = (Map<String, Object>) request.getSession()
          .getAttribute(SplibWebConstants.KEY_CONTEXT_MAP_PREFIX + contextId);

      // when contextId is appended to the URL
      // maybe because of the reuse of the URL with contextId but no contextMap is saved.
      if (contextMap != null) {
        // model
        Model redirectFromModel =
            (Model) ObjectsUtil.requireNonNull(contextMap.get(SplibWebConstants.KEY_MODEL));
        Map<String, Object> modelMapRedirectFrom = redirectFromModel.asMap();
        modelMapRedirectFrom.remove(SplibWebConstants.KEY_MESSAGES_BEAN);
        model.addAllAttributes(modelMapRedirectFrom);

        // messageBean
        model.addAttribute(SplibWebConstants.KEY_MESSAGES_BEAN,
            contextMap.get(SplibWebConstants.KEY_MESSAGES_BEAN));

        // Retrieve forms from model and change settings.
        // Validation should have been performed before the redirect if needed,
        // so there is no need to re-validate when reusing.
        model.asMap().values().stream()
            .filter(SplibGeneralForm.class::isInstance)
            .map(SplibGeneralForm.class::cast)
            .forEach(SplibGeneralForm::noValidate);

        // remove contextMap from session
        request.getSession()
            .removeAttribute(SplibWebConstants.KEY_CONTEXT_MAP_PREFIX + contextId);
      }
    }

    // add new MessagesBean if null
    if (model.getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN) == null) {
      model.addAttribute(SplibWebConstants.KEY_MESSAGES_BEAN, new MessagesBean());
    }
  }
}
