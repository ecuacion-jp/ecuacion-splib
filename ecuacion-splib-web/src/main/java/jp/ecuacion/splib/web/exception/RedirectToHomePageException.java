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

import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

/**
 * Transitions to home page.
 */
public class RedirectToHomePageException extends RedirectException {

  private static final long serialVersionUID = 1L;
  private static final String HOME_PAGE_PATH =
      PropertiesFileUtil.getApplication("jp.ecuacion.splib.web.home-page");

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException() {
    this((@Nullable Level) null, (@Nullable String) null, (@Nullable String) null);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(String messageId, String... messageArgs) {
    this((@Nullable Level) null, (@Nullable String) null, messageId, messageArgs);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(@Nullable Level logLevel, @Nullable String logString) {
    this(logLevel, logString, (@Nullable String) null);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(@Nullable Level logLevel, @Nullable String logString,
      @Nullable String messageId, String... messageArgs) {
    super(properHomePagePath(), logLevel, logString, messageId, messageArgs);
  }

  /**
   * See {@link RedirectException}.
   */
  public RedirectToHomePageException(@Nullable Level logLevel, String messageId,
      String[] messageArgs) {
    super(properHomePagePath(), logLevel, messageId, messageArgs);
  }

  private static String properHomePagePath() {
    return (HOME_PAGE_PATH.startsWith("/") ? "" : "/") + HOME_PAGE_PATH;
  }
}
