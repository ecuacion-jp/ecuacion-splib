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
   * @param itemIds itemIds
   */
  public void setErrorMessage(String message, String... itemIds) {
    errorMessageList.add(new ErrorMessageBean(message, itemIds));
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
   * Obtains all the messages related to the specified itemId.
   */
  public List<String> getErrorMessages(String itemId) {
    return getErrorMessageList(itemId);
  }

  /**
   * Obtains all the messages with itemId specified.
   */
  public List<String> getErrorMessagesLinkedToItems() {
    return errorMessageList.stream().filter(e -> e.getItemNameSet().size() > 0)
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  /**
   * Obtains all the messages without itemId specified.
   */
  public List<String> getErrorMessagesNotLinkedToItems() {
    return errorMessageList.stream().filter(e -> e.getItemNameSet().size() == 0)
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  /**
   * Returns invalid class string if the item is invalid.
   * 
   * @param itemId itemId
   * @return String
   */
  public String isValid(String itemId) {
    return (getErrorMessageList(itemId).size() > 0) ? "is-invalid" : "";
  }

  /**
   * Stores the pair of error messages and itemIds.
   * 
   * <p>The pair can be (message : itemId) = 1:1, 1:0, 1:n.</p>
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
