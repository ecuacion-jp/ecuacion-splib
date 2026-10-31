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
package jp.ecuacion.splib.core.audit;

import jp.ecuacion.splib.core.audit.AuditLogWriter.TargetData;

/**
 * Adds information to the audit log record of the operation currently being processed.
 *
 * <p>Call from a controller, service or BL while handling a request recorded by
 *     {@code ecuacion-splib-webmvc}'s {@code AuditLogInterceptor}, for example:</p>
 *
 * <pre>{@code
 * AuditLogUtil.addTarget("Inquiry", inquiry.getId());
 * AuditLogUtil.putAttribute("systemCode", inquiry.getSystemCode());
 * }</pre>
 *
 * <p>Both methods do nothing when the current thread is not handling a recorded request — the
 *     audit log is disabled, the request is not recorded (e.g. a GET), or the code runs in batch —
 *     so they can be called unconditionally.</p>
 *
 * <p>The information is kept per thread. If the operation hands work off to another thread
 *     (e.g. {@code @Async}), call these methods before that, on the request's thread.</p>
 */
public final class AuditLogUtil {

  private AuditLogUtil() {}

  /**
   * Adds a piece of data the current operation is about. Adding the same data again does
   * nothing.
   *
   * <p>Some are added automatically: {@code ecuacion-splib-web} adds the record its forms are
   *     about (the record the edit page shows, the row whose button was pressed on the list
   *     page), and {@code ecuacion-splib-web-jpa}'s {@code SplibEditJpaService#insertOrUpdate}
   *     adds the saved entity. Add others — rows changed by an application's own service, rows
   *     deleted by a bulk query — with this method.</p>
   *
   * @param type the kind of the data, such as the entity name
   * @param id the identifier of the data, such as the primary key. Recorded as
   *     {@code String.valueOf(id)}.
   */
  public static void addTarget(String type, Object id) {
    AuditLogContext context = AuditLogContext.current();
    if (context != null) {
      context.addTarget(new TargetData(type, String.valueOf(id)));
    }
  }

  /**
   * Adds an application-specific item to the current operation's record. A later call with the
   * same {@code key} overwrites the value.
   *
   * <p>Do not pass secrets or personal data the audit log should not keep.</p>
   *
   * @param key the item name
   * @param value the value
   */
  public static void putAttribute(String key, String value) {
    AuditLogContext context = AuditLogContext.current();
    if (context != null) {
      context.putAttribute(key, value);
    }
  }
}
