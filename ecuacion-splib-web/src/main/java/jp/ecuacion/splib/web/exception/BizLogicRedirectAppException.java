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
package jp.ecuacion.splib.web.exception;

import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.splib.web.bean.RedirectUrlBean;

/** 
 * redirectを行いredirect先でエラーメッセージを表示するために使用するException。
 * redirect先にはErrorFieldはなし、またwebで使用する前提でありlocaleも不要のため
 * constructorは下記2種類のみとなる。
 */
public class BizLogicRedirectAppException extends BizLogicAppException {
  private static final long serialVersionUID = 1L;

  private RedirectUrlBean redirectUrlBean;

  public BizLogicRedirectAppException(RedirectUrlBean redirectUrlBean, String messageId) {
    super(messageId);
    this.redirectUrlBean = redirectUrlBean;
  }

  public BizLogicRedirectAppException(RedirectUrlBean redirectUrlBean, String messageId,
      String... messageArgs) {
    super(messageId, messageArgs);
    this.redirectUrlBean = redirectUrlBean;
  }

  public RedirectUrlBean getRedirectUrlBean() {
    return redirectUrlBean;
  }
}
