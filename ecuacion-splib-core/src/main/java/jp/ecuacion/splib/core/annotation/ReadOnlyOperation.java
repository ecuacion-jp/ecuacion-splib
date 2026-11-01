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
package jp.ecuacion.splib.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies whether the operations a method handles — for a Spring MVC application, requests to
 *     a controller method — change nothing (only show data).
 *
 * <p>Which operations change nothing without this annotation is decided by the module that
 *     handles them. For a Spring MVC application ({@code ecuacion-splib-webmvc}), requests with
 *     the HTTP method {@code GET}, {@code HEAD}, {@code OPTIONS} or {@code TRACE} change nothing.
 *     Use it to change that for a method or a whole controller:</p>
 *
 * <ul>
 *   <li>{@code @ReadOnlyOperation}: changes nothing, for a {@code POST} that only shows a page
 *       (e.g. going back to the previous page, opening the detail page from a list).</li>
 *   <li>{@code @ReadOnlyOperation(false)}: changes something, for a {@code GET} that writes.</li>
 * </ul>
 *
 * <p>It is used for:</p>
 *
 * <ul>
 *   <li>the audit log: operations that change nothing are not recorded, unless
 *       {@code @AuditLogTarget} says otherwise.</li>
 *   <li>the switched user ({@code ecuacion-splib-web}'s
 *       {@code SplibWebSecurityConfigForSwitchUser#isReadOnlyWhileSwitched()}): while switched,
 *       only operations that change nothing are allowed.</li>
 * </ul>
 *
 * <p>Looked up on the method first (including the method it overrides), then on the class. Methods
 *     of {@code ecuacion-splib}'s own controllers that change nothing with a {@code POST} already
 *     carry it, which an overriding method in the application inherits. Do not put it on a method
 *     that changes something: while switched it would be allowed.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ReadOnlyOperation {

  /**
   * Returns whether the operations change nothing.
   *
   * @return {@code true} if they change nothing
   */
  boolean value() default true;
}
