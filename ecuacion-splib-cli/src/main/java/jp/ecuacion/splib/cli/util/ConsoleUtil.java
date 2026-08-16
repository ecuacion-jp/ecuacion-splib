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
package jp.ecuacion.splib.cli.util;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Provides console output shared by {@code SplibCliApplication} and {@code SplibExceptionHandler}
 * for the start / completion / error messages an ecuacion-splib-cli app prints while running —
 * each is prefixed with a {@code [yyyy-MM-dd HH:mm:ss]} timestamp so the user can tell at a
 * glance how long the run (or a step of it) took.
 */
public final class ConsoleUtil {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private ConsoleUtil() {}

  /**
   * Prints {@code message} to {@code out}, prefixed with the current local time as
   * {@code [yyyy-MM-dd HH:mm:ss]}.
   *
   * @param isError true when the print should be done to stderr
   * @param message the message to print
   */
  public static void printlnWithTimestamp(boolean isError, String message) {
    PrintStream out = isError ? System.err : System.out;
    Objects.requireNonNull(out);
    out.println("[" + LocalDateTime.now(ZoneId.systemDefault()).format(TIMESTAMP_FORMATTER) + "] "
        + message);
  }
}
