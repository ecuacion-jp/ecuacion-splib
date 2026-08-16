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
package jp.ecuacion.splib.cli.runner;

/**
 * Provides the single entry point an ecuacion-splib-cli app implements to run its logic once.
 *
 * <p>Unlike {@code ecuacion-splib-batch}, which splits work into Spring Batch Jobs and Steps
 *     for unattended, scheduler-triggered execution, a CLI app is run directly by a user and
 *     watched interactively, so it has no need for that kind of division: an app normally
 *     provides a single {@code @Component}-annotated implementation.</p>
 *
 * <p>Named {@code Runner} (echoing Spring Boot's own {@code CommandLineRunner}/
 *     {@code ApplicationRunner}: "runs once after startup") rather than {@code Command},
 *     deliberately leaving that name free for a possible future interactive/REPL-style CLI
 *     entry point, where "Command" would naturally mean one of many commands dispatched inside
 *     a read-eval-print loop — a different concept from this single, whole-app entry point.</p>
 */
public interface SplibCliRunner {

  /**
   * Runs the app's main logic.
   *
   * @param args the command-line arguments passed to {@code main}
   * @throws Exception any exception raised while running; caught and handled by
   *     {@code SplibExceptionHandler}
   */
  void execute(String[] args) throws Exception;
}
