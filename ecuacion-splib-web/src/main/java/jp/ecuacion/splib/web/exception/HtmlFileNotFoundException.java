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
 * Notices that the html page file specified by url parameter does not exist.
 */
public class HtmlFileNotFoundException extends RedirectToHomePageException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new instance.
   * 
   * @param filename filename
   */
  public HtmlFileNotFoundException(String filename) {
    this(Level.ERROR, filename);
  }

  /**
   * Constructs a new instance.
   * 
   * @param logLevel log level
   * @param filename filename
   */
  public HtmlFileNotFoundException(Level logLevel, String filename) {
    super(logLevel,
        "Designated html file not found. html file name = templates/" + filename + ".html");
  }
}
