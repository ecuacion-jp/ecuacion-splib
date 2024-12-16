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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

@ControllerAdvice
public class SplibControllerAdvice {
  public static final String REQUEST_KEY_MODEL = "REQUEST_KEY_MODEL";

  @Autowired
  protected HttpServletRequest request;

  @Autowired
  private SplibModelAttributes modelAttr;

  @Autowired
  SplibUtil util;

  @ModelAttribute
  public void setObjectsToModel(Model model) {

    // modelをrequestに追加。exceptionHandlerでModelを使いたいのだが、
    // そこでmodelをDIで取得すると中身が空っぽのModelが取得されるため。
    request.setAttribute(REQUEST_KEY_MODEL, model);

    // redirect元から引き継がれたmodel, messagesを復元しmodelに追加
    recoverRequestParametersFromRedirectContextId(model);

    // 今回の処理で呼ばれるcontrollerの処理の最後でredirectをする場合に使用されるためのcontextIdなどの準備
    prepareRedirectContextId(model);

    // transactionToken
    model.addAttribute(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN,
        new TransactionTokenUtil().issueNewToken(request));

    // url pathを追加。thymeleafでrequestの直接使用が不可になり、urlなどの使用はcontrollerでのmodelへの設定が推奨とのこと。
    model.addAttribute("loginState", util.getLoginState());

    // 個別appの中で、全画面共通で使用したいparameterを設定。
    modelAttr.addAllToModel(model);
  }

  private void recoverRequestParametersFromRedirectContextId(Model model) {
    // messages, modelをredirect元から引き継いでいる場合はmodelに追加。
    String contextId = request.getParameter(SplibWebConstants.KEY_CONTEXT_ID);
    if (contextId != null && !contextId.equals("")) {
      // url parameterでmessagesBeanKeyUuidが渡された場合のみの処理。
      // uuidは渡されたが、sessionが切れたなどでbeanMapが存在しない場合もあるため、beanMapが存在する際のみ実施

      // messageBean
      @SuppressWarnings("unchecked")
      Map<String, MessagesBean> messageBeanMap = (Map<String, MessagesBean>) request.getSession()
          .getAttribute(SplibWebConstants.KEY_MESSAGES_MAP);

      if (messageBeanMap != null && messageBeanMap.get(contextId) != null) {
        model.addAttribute(MessagesBean.key, messageBeanMap.get(contextId));

        // 一度使ったmessagesBeanKeyUuidに対するものは、二度とは使用しないためにmapから削除。他に残っているものがあればそれ模索するためclearする
        messageBeanMap.clear();
      }

      // model
      @SuppressWarnings("unchecked")
      final Map<String, Model> modelMap = (HashMap<String, Model>) request.getSession()
          .getAttribute(SplibWebConstants.KEY_MODEL_MAP);

      if (modelMap != null && modelMap.get(contextId) != null) {
        // 別途渡しているrequestResultと重複してしまうので、modelからrequestResultを除いておく
        Map<String, Object> modelMapRedirectFrom = modelMap.get(contextId).asMap();
        modelMapRedirectFrom.remove(MessagesBean.key);
        model.addAllAttributes(modelMapRedirectFrom);

        // 一度使ったmessagesBeanKeyUuidに対するものは、二度とは使用しないためにmapから削除。他に残っているものがあればそれ模索するためclearする
        modelMap.clear();
      }
    }

    // messagesBeanが引き継がれていない場合は、新規のmessagesBeanを設定しておく
    if (!model.containsAttribute(MessagesBean.key)) {
      model.addAttribute(MessagesBean.key, new MessagesBean());
    }

    // form。
    // validationは必要ならredirectの前に実施されているはずなので、再利用する際に再度validationを行う必要はない
    model.asMap().entrySet().stream().filter(e -> e.getValue() instanceof SplibGeneralForm)
        .forEach(e -> ((SplibGeneralForm) e.getValue()).noValidate());
  }

  /** redirectされた先で必要情報を受け取れるようにするための準備。 */
  private void prepareRedirectContextId(Model model) {
    // contextId
    String contextId = UUID.randomUUID().toString();
    request.getSession().setAttribute(SplibWebConstants.KEY_CONTEXT_ID, contextId);

    // messagesBean
    @SuppressWarnings("unchecked")
    Map<String, MessagesBean> messagesMap = (HashMap<String, MessagesBean>) request.getSession()
        .getAttribute(SplibWebConstants.KEY_MESSAGES_MAP);
    if (messagesMap == null) {
      messagesMap = new HashMap<String, MessagesBean>();
      request.getSession().setAttribute(SplibWebConstants.KEY_MESSAGES_MAP, messagesMap);
    }

    final MessagesBean requestResult = ((MessagesBean) model.getAttribute(MessagesBean.key));

    // messagesBeanは常に設定
    messagesMap.put(contextId.toString(), requestResult);

    // modelMap
    @SuppressWarnings("unchecked")
    Map<String, Model> modelMap =
        (HashMap<String, Model>) request.getSession().getAttribute(SplibWebConstants.KEY_MODEL_MAP);
    if (modelMap == null) {
      modelMap = new HashMap<String, Model>();
      request.getSession().setAttribute(SplibWebConstants.KEY_MODEL_MAP, modelMap);
    }

    // modelはredirect先への引き継ぎ有無が場合によるためここではmodelMapへのmodel設定はしない

  }
}
