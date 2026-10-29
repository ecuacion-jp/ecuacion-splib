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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies whether the operations a method handles — for a Spring MVC application, requests to
 *     a controller method — are recorded in the audit log.
 *
 * <p>Which operations are recorded without this annotation is decided by the module that records
 *     them. For a Spring MVC application ({@code ecuacion-splib-webmvc}), requests with an HTTP
 *     method other than {@code GET}, {@code HEAD}, {@code OPTIONS} and {@code TRACE} are recorded.
 *     Use it to change that for a method or a whole controller:</p>
 *
 * <ul>
 *   <li>{@code @AuditLogTarget(false)}: not recorded, for a {@code POST} that changes nothing
 *       (e.g. going back to the previous page).</li>
 *   <li>{@code @AuditLogTarget}: recorded, for a {@code GET} worth recording (e.g.
 *       downloading personal data).</li>
 * </ul>
 *
 * <p>Looked up on the method first (including the method it overrides), then on the class. Methods
 *     of {@code ecuacion-splib}'s own controllers that change nothing already carry
 *     {@code @AuditLogTarget(false)}, which an overriding method in the application inherits.
 *     </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AuditLogTarget {

  /**
   * Returns whether requests are recorded.
   *
   * @return {@code true} if recorded
   */
  boolean value() default true;
}
