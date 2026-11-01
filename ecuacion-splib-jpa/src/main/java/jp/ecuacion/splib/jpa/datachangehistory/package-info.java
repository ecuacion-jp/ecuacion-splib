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
 * Provides the data change history: the history of the data changed through JPA, kept with
 * Hibernate Envers for the entities annotated with {@code @Audited}.
 *
 * <p>Enabled by adding {@code hibernate-envers} to the application. Each change
 *     is kept in the entity's history table ({@code <table>_aud}), and who made it, when, and
 *     by which operation in {@link DataChangeRevision} ({@code revinfo}).</p>
 */
@NullMarked
package jp.ecuacion.splib.jpa.datachangehistory;

import org.jspecify.annotations.NullMarked;
