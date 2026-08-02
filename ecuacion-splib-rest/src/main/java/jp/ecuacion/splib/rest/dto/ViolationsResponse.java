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
package jp.ecuacion.splib.rest.dto;

import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Response body for a {@code jp.ecuacion.lib.core.exception.ViolationException}, carrying every
 * violation's message — already resolved and localized to the request's locale — for direct
 * display to a human end user.
 *
 * @param messages the localized violation messages, in the same order as
 *     {@code Violations.getConstraintViolations()} followed by
 *     {@code Violations.getBusinessViolations()}
 */
public record ViolationsResponse(List<@NonNull String> messages) {
}
