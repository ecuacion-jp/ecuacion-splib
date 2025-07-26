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

import org.slf4j.event.Level;

/** 
 * Is an exception extending {@code BizLogicAppException}.
 * 
 * <p>This causes redirect to the other page and show error messages in it.<br>
 * Since the page is transitioned, the error item is not supposed to exist.
 * Therefore this exception doesn't have {@code AppErrorFields}.</p>
 */
public class RedirectException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private String redirectPath;
  private String messageId;
  private String[] messageArgs;
  private Level logLevel;
  private String logString;

  /**
   * Constructs a new instance.
   * 
   * @param redirectPath redirectPath like "/account/instancePowerStatus/searchList/page".
   */
  public RedirectException(String redirectPath) {

  }

  /**
   * Constructs a new instance.
   * 
   * @param redirectPath redirectPath like "/account/instancePowerStatus/searchList/page".
   * @param messageId messageId
   * @param messageArgs messageArgs
   */
  public RedirectException(String redirectPath, String messageId, String... messageArgs) {
    this.redirectPath = redirectPath;
    this.messageId = messageId;
    this.messageArgs = messageArgs;
  }

  /**
   * Constructs a new instance.
   * 
   * @param redirectPath redirectPath like "/account/instancePowerStatus/searchList/page".
   * @param logLevel logLevel
   * @param logString logString
   */
  public RedirectException(String redirectPath, Level logLevel, String logString) {
    this.redirectPath = redirectPath;
    this.logLevel = logLevel;
    this.logString = logString;
  }

  /**
   * Constructs a new instance.
   * 
   * @param redirectPath redirectPath like "/account/instancePowerStatus/searchList/page".
   * @param messageId messageId
   * @param messageArgs messageArgs
   * @param logLevel logLevel
   * @param logString logString
   */
  public RedirectException(String redirectPath, Level logLevel, String logString, String messageId,
      String... messageArgs) {
    this.redirectPath = redirectPath;
    this.logLevel = logLevel;
    this.logString = logString;
    this.messageId = messageId;
    this.messageArgs = messageArgs;
  }

  public String getRedirectPath() {
    return redirectPath;
  }

  public String getMessageId() {
    return messageId;
  }

  public String[] getMessageArgs() {
    return messageArgs;
  }

  public Level getLogLevel() {
    return logLevel;
  }

  public String getLogString() {
    return logString;
  }
}
