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
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.bean.SplibModelAttributes;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibUtil;
import jp.ecuacion.splib.web.util.internal.TransactionTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Provides common procedure that are supposed to be done before controllers are called.
 */
@ControllerAdvice
public class SplibControllerAdvice {

  @Autowired
  private HttpServletRequest request;

  @Autowired
  private SplibModelAttributes modelAttr;

  @Autowired
  private SplibUtil util;

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

    // modelをrequestに追加。exceptionHandlerでModelを使いたいのだが、
    // そこでmodelをDIで取得すると中身が空っぽのModelが取得されるため。
    request.setAttribute(SplibWebConstants.KEY_MODEL, model);

    // redirect元から引き継がれたmodel, messagesを復元しmodelに追加
    recoverRequestParametersFromRedirectContextId(model);

    // transactionToken
    model.addAttribute(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN,
        new TransactionTokenUtil().issueNewToken(request));

    // url pathを追加。thymeleafでrequestの直接使用が不可になり、urlなどの使用はcontrollerでのmodelへの設定が推奨とのこと。
    model.addAttribute("loginState", util.getLoginState());

    // 個別appの中で、全画面共通で使用したいparameterを設定。
    modelAttr.addAllToModel(model);
  }

  private void recoverRequestParametersFromRedirectContextId(Model model) {

    boolean takesOverContext = true;

    // messages, modelをredirect元から引き継いでいる場合はmodelに追加。
    String contextId = request.getParameter(SplibWebConstants.KEY_CONTEXT_ID);
    if (contextId == null || contextId.equals("")) {
      takesOverContext = false;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> contextMap = (Map<String, Object>) request.getSession()
        .getAttribute(SplibWebConstants.KEY_CONTEXT_MAP_PREFIX + contextId);
    // when contextId is appended to the URL
    // maybe because of the reuse of the URL with contextId but no contextMap is saved.
    if (contextMap == null) {
      takesOverContext = false;
    }

    if (takesOverContext) {
      // model
      Map<String, Object> modelMapRedirectFrom =
          ((Model) contextMap.get(SplibWebConstants.KEY_MODEL)).asMap();
      modelMapRedirectFrom.remove(SplibWebConstants.KEY_MESSAGES_BEAN);
      model.addAllAttributes(modelMapRedirectFrom);

      // messageBean
      model.addAttribute(SplibWebConstants.KEY_MESSAGES_BEAN,
          contextMap.get(SplibWebConstants.KEY_MESSAGES_BEAN));

      // formをmodelから取得し設定変更
      // validationは必要ならredirectの前に実施されているはずなので、再利用する際に再度validationを行う必要はない
      model.asMap().entrySet().stream().filter(e -> e.getValue() instanceof SplibGeneralForm)
          .forEach(e -> ((SplibGeneralForm) e.getValue()).noValidate());

      // remove contextMap from session
      request.getSession().removeAttribute(SplibWebConstants.KEY_CONTEXT_MAP_PREFIX + contextId);
    }

    // add new MessagesBean if null
    if (model.getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN) == null) {
      model.addAttribute(SplibWebConstants.KEY_MESSAGES_BEAN, new MessagesBean());
    }
  }
}
