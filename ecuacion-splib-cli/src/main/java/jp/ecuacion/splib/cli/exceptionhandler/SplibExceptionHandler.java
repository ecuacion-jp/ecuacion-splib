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
package jp.ecuacion.splib.cli.exceptionhandler;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LocaleUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.cli.util.ConsoleUtil;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.ui.util.SplibViolationUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Provides an exception handler for ecuacion-splib-cli apps.
 *
 * <p>Unlike {@code ecuacion-splib-web} (where implementing an exception handler is left up to
 *     the app) or {@code ecuacion-splib-batch} (where this class is a fixed {@code @Component}
 *     plugged into Spring Batch's own {@code ExceptionHandler} SPI), this class is invoked
 *     directly by {@code SplibCliApplication#main} around the app's single
 *     {@code SplibCliCommand} execution.</p>
 *
 * <p>Since a CLI app is watched interactively by the user running it, the console message is
 *     kept short and user-facing by default; no stack trace is printed unless {@code verbose}
 *     is requested (see {@link #handle}). The full detail is always written via
 *     {@link LogUtil#logSystemError} — by default this reaches no destination at all
 *     (ecuacion-splib-cli's recommended default logback config attaches no appender anywhere),
 *     but it costs nothing and "just works" the moment an app configures its own logger/appender
 *     (e.g. to write a log file after all). An app that needs to go further — e.g. notifying a
 *     developer/admin so they can investigate — supplies a {@link SplibExceptionHandlerAction},
 *     which receives the same {@code Throwable}.</p>
 */
@Component
public class SplibExceptionHandler {

  private static final PrintStream err = Objects.requireNonNull(System.err);
  private final DetailLogger detailLog = new DetailLogger(this);

  @Nullable
  private final SplibExceptionHandlerAction action;

  /**
   * Constructs a new instance.
   *
   * @param action action, may be {@code null}
   */
  public SplibExceptionHandler(@Nullable SplibExceptionHandlerAction action) {
    this.action = action;
  }

  /**
   * Handles an uncaught {@code Throwable} raised while running the app's {@code SplibCliRunner}.
   *
   * @param throwable throwable
   * @param verbose when {@code true} (the app was run with {@code --verbose}), also prints
   *     {@code throwable}'s full stack trace to {@code System.err}, on top of the concise
   *     message always shown
   */
  public void handle(Throwable throwable, boolean verbose) {
    if (throwable instanceof ViolationException violationException) {
      printViolationMessages(violationException);

    } else {
      ConsoleUtil.printlnWithTimestamp(true,
          PropertiesFileUtil.getMessage(LocaleUtil.getFallbackLocale(),
              "jp.ecuacion.splib.cli.common.message.unexpectedError",
              String.valueOf(throwable.getMessage())));
    }

    LogUtil.logSystemError(detailLog, throwable);

    if (verbose) {
      throwable.printStackTrace();
    }

    if (action != null) {
      try {
        Objects.requireNonNull(action).execute(throwable);

      } catch (Throwable th) {
        LogUtil.logSystemError(detailLog, th);
      }
    }
  }

  private void printViolationMessages(ViolationException violationException) {
    ConsoleUtil.printlnWithTimestamp(true, PropertiesFileUtil.getMessage(
        LocaleUtil.getFallbackLocale(), "jp.ecuacion.splib.cli.common.message.violationOccurred"));

    // Filter out ConstraintViolations already masked by a required-field BusinessViolation
    // on the same item, matching ecuacion-splib-web's error display convention.
    Violations filtered = SplibViolationUtil.excludeConstraintViolationsMaskedByRequiredError(
        violationException.getViolations(), Function.identity());
    List<@NonNull String> msgList =
        ExceptionUtil.getMessageList(filtered, LocaleUtil.getFallbackLocale(), true);
    msgList.forEach(msg -> err.println("  - " + msg));
  }
}
