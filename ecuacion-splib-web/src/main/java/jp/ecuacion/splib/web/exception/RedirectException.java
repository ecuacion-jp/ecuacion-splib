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

import java.util.Locale;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

/** 
 * Redirects to specified page.
 */
public class RedirectException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** redirect path. */
  private String redirectPath;

  /** messageId to show message at the redirected page. */
  @Nullable
  private String messageId;

  /** messageArgs to show message at the redirected page. */
  private String[] messageArgs = new String[] {};

  /** logLevel to log to file. */
  @Nullable
  private Level logLevel;

  /** log message to log to file. */
  @Nullable
  private String logString;

  /**
   * Constructs a new instance.
   * 
   * @param redirectPath redirectPath like "/account/instancePowerStatus/searchList/page".
   */
  public RedirectException(String redirectPath) {
    this.redirectPath = redirectPath;
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
  public RedirectException(String redirectPath, @Nullable Level logLevel,
      @Nullable String logString) {
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
  public RedirectException(String redirectPath, @Nullable Level logLevel,
      @Nullable String logString, @Nullable String messageId, String... messageArgs) {
    this.redirectPath = redirectPath;
    this.logLevel = logLevel;
    this.logString = logString;
    this.messageId = messageId;
    this.messageArgs = messageArgs;
  }

  /**
   * Constructs a new instance.
   * 
   * <p>messageId and messageArgs are used both for message shown on the page and log message.<br>
   *     English locale is always used for log messages.</p>
   * 
   * <p>Since hard to distinguish other constructors, messageArgs is not {@code String...},
   *     but {@code String[]}.</p>
   * 
   * @param redirectPath redirectPath like "/account/instancePowerStatus/searchList/page".
   * @param messageId messageId
   * @param messageArgs messageArgs
   * @param logLevel logLevel
   */
  public RedirectException(String redirectPath, @Nullable Level logLevel, String messageId,
      String[] messageArgs) {
    this.redirectPath = redirectPath;
    this.logLevel = logLevel;
    this.logString = PropertiesFileUtil.getMessage(Locale.ENGLISH, messageId, messageArgs);
    this.messageId = messageId;
    this.messageArgs = messageArgs;
  }

  public String getRedirectPath() {
    return redirectPath;
  }

  public @Nullable String getMessageId() {
    return messageId;
  }

  public String[] getMessageArgs() {
    return messageArgs;
  }

  public @Nullable Level getLogLevel() {
    return logLevel;
  }

  public @Nullable String getLogString() {
    return logString;
  }
}
