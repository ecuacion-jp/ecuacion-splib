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
package jp.ecuacion.splib.web.bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MessagesBean {

  /** 
   * DBを使用する場合は、通常データ更新などの後処理の際はredirectしそのURLの中に?successを混ぜることで処理成功メッセージを出す。
   * が、DBを使用せずserverで処理した結果をformに詰めそれを画面で表示したい場合、redirectするとformの情報が消えるため画面に表示できない。
   * その場合はredirectは使用せずreturnとしてhtmlファイル名を指定して画面表示するのだが、その場合にもredirectと同様にsuccessメッセージを出したい。
   * その場合に、本flagをtrueに設定することでメッセージが標示される。
   */
  private boolean needsSuccessMessage = false;

  private WarnMessageBean warnMessage;

  /**
   * spring mvc標準のbean validationのエラー処理を使用しない場合に使用する。 並び順を保持するためにListを使用。
   */
  private List<ErrorMessageBean> errorMessageList = new ArrayList<>();

  private List<String> getErrorMessageList(String itemName) {
    List<String> returnList = new ArrayList<>();

    for (ErrorMessageBean bean : errorMessageList) {
      if (bean.getItemNameSet().contains(itemName)) {
        returnList.add(bean.message);
      }
    }

    return returnList;
  }

  public boolean getNeedsSuccessMessage() {
    return needsSuccessMessage;
  }

  public void setNeedsSuccessMessage(boolean needsSuccessMessage) {
    this.needsSuccessMessage = needsSuccessMessage;
  }

  public WarnMessageBean getWarnMessage() {
    return warnMessage;
  }

  public void setWarnMessage(WarnMessageBean warnMessage) {
    this.warnMessage = warnMessage;
  }

  public void setWarnMessage(String messageId, String message) {
    this.warnMessage = new WarnMessageBean(messageId, message);
  }

  public void setWarnMessage(String messageId, String message, String buttonName) {
    this.warnMessage = new WarnMessageBean(messageId, message, buttonName);
  }

  public void setErrorMessage(String message) {
    errorMessageList.add(new ErrorMessageBean(message));
  }

  public void setErrorMessage(String message, String... itemName) {
    errorMessageList.add(new ErrorMessageBean(message, itemName));
  }

  /**
   * 全てのエラーを取得。
   */
  public List<String> getErrorMessages() {
    List<String> rtnList = new ArrayList<>();

    for (ErrorMessageBean bean : errorMessageList) {
      rtnList.add(bean.getMessage());
    }

    return rtnList;
  }

  /**
   * 指定されたitemに対する全てのエラーを取得。
   */
  public List<String> getErrorMessages(String itemName) {
    return getErrorMessageList(itemName);
  }

  /**
   * item指定のある全てのエラーを取得。
   */
  public List<String> getErrorMessagesLinkedToItems() {
    return errorMessageList.stream().filter(e -> e.getItemNameSet().size() > 0)
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  /**
   * item指定のない全てのエラーを取得。
   */
  public List<String> getErrorMessagesNotLinkedToItems() {
    return errorMessageList.stream().filter(e -> e.getItemNameSet().size() == 0)
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  public String isValid(String itemName) {
    return (getErrorMessageList(itemName).size() > 0) ? "is-invalid" : "";
  }

  /**
   * エラーメッセージとそのメッセージが指す項目をペアで持つ。 bean validationのような単純にメッセージと項目が1:1となる場合ばかりではなく、
   * 相関チェックだと一つのエラーで複数の項目が対象となったり、対象項目が存在しないエラーなどもある。
   * それらを網羅して保持できるため、一つのメッセージと、0以上の項目をセットで保持できるbeanとする。
   */
  static class ErrorMessageBean {
    private String message;

    private Set<String> itemNameSet = new HashSet<>();

    public ErrorMessageBean(String message) {
      this.message = message;
    }

    public ErrorMessageBean(String message, String... itemName) {
      this.message = message;
      this.itemNameSet =
          new HashSet<String>(Arrays.asList(itemName == null ? new String[] {} : itemName));
    }

    public String getMessage() {
      return message;
    }

    public Set<String> getItemNameSet() {
      return itemNameSet;
    }
  }

  public static class WarnMessageBean {
    private String messageId;
    private String message;
    private String buttonName;

    public WarnMessageBean(String messageId, String message) {
      this(messageId, message, null);
    }

    public WarnMessageBean(String messageId, String message, String buttonName) {
      this.messageId = messageId;
      this.message = message;
      this.buttonName = buttonName;
    }

    public String getMessageId() {
      return messageId;
    }

    public void setMessageId(String messageId) {
      this.messageId = messageId;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getButtonName() {
      return buttonName == null ? "" : buttonName;
    }
  }
}
