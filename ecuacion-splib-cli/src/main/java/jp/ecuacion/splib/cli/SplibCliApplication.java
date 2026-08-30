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
package jp.ecuacion.splib.cli;

import java.util.Arrays;
import jp.ecuacion.lib.core.util.LocaleUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.cli.banner.SplibCliBanner;
import jp.ecuacion.splib.cli.exceptionhandler.SplibExceptionHandler;
import jp.ecuacion.splib.cli.runner.SplibCliRunner;
import jp.ecuacion.splib.cli.util.ConsoleUtil;
import jp.ecuacion.splib.cli.util.SpinnerUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Provides default CliApplication.
 *
 * <p>We want it to be {@code abstract} to clarify
 *    that this class is supposed to be extended to use.<br>
 *    But it's not allowed by spring. The following error message obtained.<br>
 *    {@code BeanCreationException: Error creating bean with name 'splibCliApplication':
 *    Failed to instantiate [jp.ecuacion.splib.cli.SplibCliApplication]:
 *    Is it an abstract class?}</p>
 */
public class SplibCliApplication {

  /**
   * Has what needs to be done as the main method of the CLI app.
   *
   * <p>java command doesn't seem to start
   *     by calling the main method in the parent class of the class specified.<br>
   *     So you need to implement main method in the CliApplication class in your each app
   *     and call {@code SplibCliApplication.main(cls, args)} in it.</p>
   *
   * <p>Unlike {@code SplibBatchApplication}, which delegates to a Spring Batch {@code Job},
   *     this runs the app's single {@code SplibCliRunner} bean directly and exits, with no
   *     Job/Step/JobRepository involved.</p>
   *
   * <p>{@code --verbose} among {@code args} makes {@link SplibExceptionHandler} print a full
   *     stack trace on failure, on top of its normal concise message.</p>
   *
   * <p>{@code --ecuacion-system-error} among {@code args} deliberately throws a system error
   *     instead of running the app's {@code SplibCliRunner}, so the system error behavior
   *     (exception handling, logging, and so on) can be tested without requiring an actual bug
   *     — the CLI counterpart of {@code ecuacion-splib-batch}'s built-in
   *     {@code ecuacionSystemErrorJob}.</p>
   *
   * <p>A timestamped "starting" message is printed before {@code execute} runs, and a
   *     timestamped "completed successfully" message after it returns normally — see
   *     {@link ConsoleUtil}. On failure, {@link SplibExceptionHandler} prints its own
   *     timestamped message instead of the "completed" one.</p>
   */
  public static void main(Class<?> cls, String[] args) {
    main(cls, args, null);
  }

  /**
   * Same as {@link #main(Class, String[])}, but also shows the app's own name and version
   * (read via {@code VersionUtil.getVersion("")}) in a second block of the startup banner.
   *
   * @param appName the app's display name, e.g. {@code "code-generator"}
   */
  public static void main(Class<?> cls, String[] args, @Nullable String appName) {
    SpringApplication app = new SpringApplication(cls);
    app.setBanner(new SplibCliBanner(appName));
    ConfigurableApplicationContext context = app.run(args);
    boolean verbose = Arrays.asList(args).contains("--verbose");

    ConsoleUtil.printlnWithTimestamp(false, PropertiesFileUtil.getMessage(
        LocaleUtil.getFallbackLocale(), "jp.ecuacion.splib.cli.common.message.starting"));

    int exitCode;
    SpinnerUtil.start(PropertiesFileUtil.getMessage(
        LocaleUtil.getFallbackLocale(), "jp.ecuacion.splib.cli.common.message.running"));
    try {
      if (Arrays.asList(args).contains("--ecuacion-system-error")) {
        throw new RuntimeException(
            "A system error was intentionally caused for testing purposes.");
      }

      context.getBean(SplibCliRunner.class).execute(args);
      exitCode = 0;
      ConsoleUtil.printlnWithTimestamp(false, PropertiesFileUtil.getMessage(
          LocaleUtil.getFallbackLocale(), "jp.ecuacion.splib.cli.common.message.completed"));

    } catch (Throwable th) {
      // SplibExceptionHandler prints through System.out/err, which the spinner has wrapped
      // to clear its own line first, so the error message still appears cleanly.
      context.getBean(SplibExceptionHandler.class).handle(th, verbose);
      exitCode = 1;

    } finally {
      SpinnerUtil.stop();
    }

    final int finalExitCode = exitCode;
    System.exit(SpringApplication.exit(context, () -> finalExitCode));
  }
}
