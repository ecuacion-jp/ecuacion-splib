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

/**
 * Holds the constants of the audit log and the data change history, read by more than one
 * module.
 */
public final class AuditLogConstants {

  /**
   * Enables the audit log, the record of operations ({@code ecuacion-splib-webmvc}). Disabled by
   * default.
   */
  public static final String PROP_KEY_ENABLED = "jp.ecuacion.splib.core.audit-log.enabled";

  /**
   * A class of Hibernate Envers, whose presence on the classpath enables the data change
   * history, the history of the data changed through JPA ({@code ecuacion-splib-jpa}).
   * Who changed the data is taken from the operation {@code ecuacion-splib-webmvc} is
   * processing, so it checks this class too.
   */
  public static final String DATA_CHANGE_HISTORY_CLASS = "org.hibernate.envers.Audited";

  private AuditLogConstants() {}
}
