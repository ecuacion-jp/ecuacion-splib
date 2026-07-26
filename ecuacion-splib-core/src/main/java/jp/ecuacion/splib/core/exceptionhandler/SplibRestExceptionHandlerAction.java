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
package jp.ecuacion.splib.core.exceptionhandler;

/**
 * Provides the app-dependent action when an exception occurs in {@code ecuacion-splib-rest},
 * for apps that need behavior different from {@link SplibExceptionHandlerAction}
 * in their other frontends (web, batch).
 *
 * <p>Register a bean of this type only when the REST frontend needs its own behavior.
 * A bean of {@link SplibExceptionHandlerAction} registered without this one continues to be
 * used by the REST frontend as before.</p>
 */
public interface SplibRestExceptionHandlerAction {

  /**
   * Provide the app-dependent action.
   *
   * @param th throwable
   */
  public void execute(Throwable th);

}
