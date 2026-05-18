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

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Stores a warn message that requires user confirmation before resubmitting.
 */
public class WarnMessageBean {
  private String messageId;
  private String message;
  @Nullable
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
  public WarnMessageBean(String messageId, String message, @Nullable String buttonName) {
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
    return buttonName == null ? "" : Objects.requireNonNull(buttonName);
  }
}
