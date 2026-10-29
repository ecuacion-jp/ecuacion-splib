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

/**
 * Provides the audit log of Spring MVC applications: one record per operation (who did what,
 * from where, to which data, and with what result), plus login, login failure, logout and
 * switch-user events.
 *
 * <p>Enabled by {@code jp.ecuacion.splib.core.audit-log.enabled=true} (disabled by default, in
 *     which case no bean in this package is registered at all). See {@link AuditLogConfig}
 *     for what gets registered and recorded. Scanned by {@code SplibWebConfig} and
 *     {@code SplibRestConfig}.</p>
 *
 * <p>The record, where it is written, and {@code AuditLogUtil} (through which applications add
 *     data to the record) do not depend on Spring MVC and are in {@code ecuacion-splib-core}'s
 *     {@code jp.ecuacion.splib.core.audit}.</p>
 */
@NullMarked
package jp.ecuacion.splib.webmvc.audit;

import org.jspecify.annotations.NullMarked;
