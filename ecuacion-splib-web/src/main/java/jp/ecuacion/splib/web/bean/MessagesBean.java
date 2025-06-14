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
import org.apache.commons.lang3.StringUtils;

/**
 * Stores messages shown on pages.
 */
public class MessagesBean {

  /** 
   * Shows the message which describes the procedure has been done successfully.
   * 
   * <p>When the PRG (Post Redirect Get), showing success messages are realized 
   *     by add url parameter {@code ?success}.<br>
   *     This library assumes that all the transition with messages uses PRG,
   *     but this function leaves for some cases.</p>
   */
  private boolean needsSuccessMessage = false;

  private WarnMessageBean warnMessage;

  /**
   * Stores error messages. The data type is {@code List} because the order matters.
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

  /**
   * Sets error message.
   * 
   * @param message message
   * @param itemKindIds itemKindIds
   */
  public void setErrorMessage(String message, String... itemKindIds) {
    errorMessageList.add(new ErrorMessageBean(message, itemKindIds));
  }

  /**
   * Obtains all the errors.
   */
  public List<String> getErrorMessages() {
    List<String> rtnList = new ArrayList<>();

    for (ErrorMessageBean bean : errorMessageList) {
      rtnList.add(bean.getMessage());
    }

    return rtnList;
  }

  /**
   * Obtains all the messages related to the specified itemKindId.
   */
  public List<String> getErrorMessages(String itemKindId) {
    return getErrorMessageList(itemKindId);
  }

  /**
   * Obtains all the messages with itemKindId specified.
   */
  public List<String> getErrorMessagesLinkedToItems() {
    return errorMessageList.stream().filter(e -> e.getItemNameSet().size() > 0)
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  /**
   * Obtains all the messages without itemKindId specified.
   */
  public List<String> getErrorMessagesNotLinkedToItems() {
    return errorMessageList.stream().filter(e -> e.getItemNameSet().size() == 0)
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  /**
   * Returns invalid class string if the item is invalid.
   * 
   * @param itemKindId itemKindId
   * @return String
   */
  public String isValid(String itemKindId) {
    return (getErrorMessageList(itemKindId).size() > 0) ? "is-invalid" : "";
  }

  /**
   * Stores the pair of error messages and itemKindIds.
   * 
   * <p>The pair can be (message : itemKindId) = 1:1, 1:0, 1:n.</p>
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
          new HashSet<String>(Arrays.asList(itemName == null ? new String[] {} : itemName).stream()
              .map(itemKindId -> StringUtils.uncapitalize(itemKindId)).toList());
    }

    public String getMessage() {
      return message;
    }

    public Set<String> getItemNameSet() {
      return itemNameSet;
    }
  }

  /**
   * Stores a warn message.
   */
  public static class WarnMessageBean {
    private String messageId;
    private String message;
    private String buttonName;

    /**
     * Constructs a new instance.
     * 
     * @param messageId messageId
     * @param message message
     */
    public WarnMessageBean(String messageId, String message) {
      this(messageId, message, null);
    }

    /**
     * Constructs a new instance.
     * 
     * @param messageId messageId
     * @param message message
     * @param buttonName buttonIdToPressOnConfirm
     */
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
