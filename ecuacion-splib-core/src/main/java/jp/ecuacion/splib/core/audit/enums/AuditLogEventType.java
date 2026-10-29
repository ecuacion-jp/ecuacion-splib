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
package jp.ecuacion.splib.core.audit.enums;

/**
 * Designates the kind of an audit log record.
 */
public enum AuditLogEventType {

  /** A request to a controller method, such as registering or deleting data. */
  OPERATION,

  /**
   * A login attempt, including an {@code X-Api-Key} authentication attempt on
   * {@code ecuacion-splib-rest}. Whether it succeeded is shown by {@link AuditLogResult}.
   */
  LOGIN,

  /** A logout. */
  LOGOUT,

  /** The start of a switch user (an administrator acting as another user). */
  SWITCH_USER_START,

  /** The end of a switch user (back to the original administrator). */
  SWITCH_USER_EXIT
}
