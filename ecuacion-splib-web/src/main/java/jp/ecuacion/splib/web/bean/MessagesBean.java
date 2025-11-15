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
   * @param itemPropertyPaths itemPropertyPaths
   */
  public void setErrorMessage(String message, boolean isShownAtEachItem,
      String... itemPropertyPaths) {
    errorMessageList.add(new ErrorMessageBean(message, isShownAtEachItem, itemPropertyPaths));
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
  * Obtains all the messages related to the specified itemPropertyPath.
  */
  private String[] getErrorMessages(String itemPropertyPath) {
    List<String> returnList = new ArrayList<>();

    for (ErrorMessageBean bean : errorMessageList) {
      if (bean.getItemPropertyPathSet().contains(itemPropertyPath)) {
        returnList.add(bean.message);
      }
    }

    return returnList.toArray(new String[returnList.size()]);
  }

  /**
   * Obtains all the messages with itemPropertyPath specified.
   */
  public List<String> getErrorMessagesAtEachItem() {
    return errorMessageList.stream()
        .filter(e -> e.getIsShownAtEachItem() && e.getItemPropertyPathSet().size() > 0)
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  /**
   * Obtains all the messages with itemPropertyPath specified.
   */
  public List<String> getErrorMessagesAtEachItem(String itemPropertyPath) {
    return errorMessageList.stream()
        .filter(e -> e.getIsShownAtEachItem() && e.getItemPropertyPathSet().size() > 0)
        .filter(e -> e.getItemPropertyPathSet().contains(itemPropertyPath))
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  /**
   * Obtains all the messages without itemPropertyPath specified.
   */
  public List<String> getErrorMessagesAtTheTop() {
    return errorMessageList.stream()
        .filter(e -> !(e.getIsShownAtEachItem() && e.getItemPropertyPathSet().size() > 0))
        .map(e -> e.getMessage()).collect(Collectors.toList());
  }

  /**
   * Returns invalid class string if the item is invalid.
   * 
   * @param itemPropertyPath itemPropertyPath
   * @return String
   */
  public String isValid(String itemPropertyPath) {
    return (getErrorMessages(itemPropertyPath).length > 0) ? "is-invalid" : "";
  }

  /**
   * Stores the pair of error messages and itemPropertyPaths.
   * 
   * <p>The pair can be (message : itemPropertyPath) = 1:1, 1:0, 1:n.</p>
   */
  static class ErrorMessageBean {
    private String message;
    private boolean isShownAtEachItem;
    private Set<String> itemPropertyPathSet = new HashSet<>();

    public ErrorMessageBean(String message) {
      this.message = message;
    }

    public ErrorMessageBean(String message, boolean isShownAtEachItem,
        String... itemPropertyPaths) {
      this.message = message;
      this.isShownAtEachItem = isShownAtEachItem;
      this.itemPropertyPathSet = new HashSet<String>(
          Arrays.asList(itemPropertyPaths == null ? new String[] {} : itemPropertyPaths).stream()
              .map(itemPropertyPath -> StringUtils.uncapitalize(itemPropertyPath)).toList());
    }

    public String getMessage() {
      return message;
    }

    public boolean getIsShownAtEachItem() {
      return isShownAtEachItem;
    }

    public Set<String> getItemPropertyPathSet() {
      return itemPropertyPathSet;
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
