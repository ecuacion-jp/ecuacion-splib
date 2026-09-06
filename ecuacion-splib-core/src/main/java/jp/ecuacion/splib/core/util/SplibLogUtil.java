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
package jp.ecuacion.splib.core.util;

import jp.ecuacion.lib.core.logging.DetailLogger;
import org.slf4j.event.Level;

/**
 * Provides utility methods to log messages via {@link DetailLogger} with indentation, so that
 * log output can visually express nesting (e.g. a process and its sub-steps).
 */
public class SplibLogUtil {

  private static final String INDENT_STRING = "  ";

  private static final String SEPARATOR_LARGE = "=========";
  private static final String SEPARATOR_MEDIUM = "------";
  private static final String SEPARATOR_SMALL = "---";

  /**
   * Logs {@code message} indented {@code indents} levels deep.
   *
   * @param detailLogger the logger to write to
   * @param logLevel the level to log at
   * @param message the message to log
   * @param indents the indent depth
   */
  public static void log(DetailLogger detailLogger, Level logLevel, String message, int indents) {
    String indentsString = "";
    for (int i = 0; i < indents; i++) {
      indentsString += INDENT_STRING;
    }

    detailLogger.log(logLevel, indentsString + message);
  }

  /**
   * Logs {@code message} indented {@code indents} levels deep, at {@code WARN} level.
   *
   * @param detailLogger the logger to write to
   * @param message the message to log
   * @param indents the indent depth
   */
  public static void warn(DetailLogger detailLogger, String message, int indents) {
    log(detailLogger, Level.INFO, message, indents);
  }

  /**
   * Logs {@code message} indented {@code indents} levels deep, at {@code INFO} level.
   *
   * @param detailLogger the logger to write to
   * @param message the message to log
   * @param indents the indent depth
   */
  public static void info(DetailLogger detailLogger, String message, int indents) {
    log(detailLogger, Level.INFO, message, indents);
  }

  /**
   * Logs {@code message} indented {@code indents} levels deep, at {@code DEBUG} level.
   *
   * @param detailLogger the logger to write to
   * @param message the message to log
   * @param indents the indent depth
   */
  public static void debug(DetailLogger detailLogger, String message, int indents) {
    log(detailLogger, Level.DEBUG, message, indents);
  }

  /**
   * Logs {@code message} indented {@code indents} levels deep, at {@code TRACE} level.
   *
   * @param detailLogger the logger to write to
   * @param message the message to log
   * @param indents the indent depth
   */
  public static void trace(DetailLogger detailLogger, String message, int indents) {
    log(detailLogger, Level.TRACE, message, indents);
  }

  /**
   * Logs a separator line of the given {@code kind} at {@code logLevel}, with no indentation.
   *
   * @param detailLogger the logger to write to
   * @param logLevel the level to log at
   * @param kind the separator kind, determining its length
   */
  public static void separator(DetailLogger detailLogger, Level logLevel, SeparatorKind kind) {
    String separator = switch (kind) {
      case LARGE -> SEPARATOR_LARGE;
      case MEDIUM -> SEPARATOR_MEDIUM;
      case SMALL -> SEPARATOR_SMALL;
    };
    log(detailLogger, logLevel, separator, 0);
  }

  /**
   * Logs a {@link SeparatorKind#MEDIUM}-length separator line at {@code logLevel}, with no
   * indentation.
   *
   * @param detailLogger the logger to write to
   * @param logLevel the level to log at
   */
  public static void separator(DetailLogger detailLogger, Level logLevel) {
    log(detailLogger, logLevel, SEPARATOR_MEDIUM, 0);
  }

  /**
   * The length of a separator line logged by {@link #separator}.
   */
  public static enum SeparatorKind {
    LARGE, MEDIUM, SMALL
  }
}
