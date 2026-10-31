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
 * Provides the technology-independent part of the audit log: the record, where it is written,
 * and {@link AuditLogUtil}, through which applications add data to the record of the operation
 * being processed.
 *
 * <p>Depends on nothing beyond {@code ecuacion-splib-core}. What records operations, logins and
 *     so on of a Spring MVC application, and what registers it all when
 *     {@code jp.ecuacion.splib.core.audit-log.enabled=true}, is in {@code ecuacion-splib-webmvc}
 *     ({@code jp.ecuacion.splib.webmvc.audit}), on which {@code ecuacion-splib-web} and
 *     {@code ecuacion-splib-rest} depend. Outside it (e.g. in batch), nothing is recorded and
 *     the static methods of {@link AuditLogUtil} simply do nothing.</p>
 */
@NullMarked
package jp.ecuacion.splib.core.audit;

import org.jspecify.annotations.NullMarked;
