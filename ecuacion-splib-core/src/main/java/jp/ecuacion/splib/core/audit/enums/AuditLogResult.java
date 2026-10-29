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
 * Designates the result of the event an audit log record is about.
 */
public enum AuditLogResult {

  /** Completed normally. */
  SUCCESS,

  /** An authentication (login, {@code X-Api-Key}) was rejected. */
  FAILURE,

  /**
   * Did not complete because of the user's input or the data's state, such as a validation
   * error ({@code ViolationException}) or a 4xx response.
   */
  BUSINESS_ERROR,

  /** Did not complete because of an unexpected exception or a 5xx response. */
  SYSTEM_ERROR
}
