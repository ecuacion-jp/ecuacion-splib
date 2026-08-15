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
package jp.ecuacion.splib.ui.util;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.ui.constant.SplibUiConstants;
import org.jspecify.annotations.NonNull;

/**
 * Provides utilities to manipulate {@code Violations}, shared by ecuacion-splib's UI modules
 * (web, cli, etc.).
 */
public class SplibViolationUtil {

  private SplibViolationUtil() {}

  /**
   * Removes {@code ConstraintViolation}s whose item property path already has a
   * required-field {@code BusinessViolation} (added by {@code SplibGeneralForm#validateNotEmpty}
   * or {@code SplibValidationHelper}'s own not-empty check, both of which run independently of
   * Jakarta Validation).
   *
   * <p>Without this, an empty field can show both the required-field error and an unrelated
   *     constraint error (e.g. {@code @Min}/{@code @Max} fail to parse the empty value as a
   *     number), which is confusing to the user.</p>
   *
   * <p>A {@code ConstraintViolation}'s {@code propertyPath} is always fully qualified from the
   *     validation root (e.g. {@code "instance.defaultIntervalMinToStop"}), while whether a
   *     {@code BusinessViolation}'s item property path is qualified the same way depends on the
   *     caller: {@code SplibGeneralForm#validateNotEmpty} leaves it relative to the record it was
   *     raised against (e.g. {@code "defaultIntervalMinToStop"}), whereas
   *     {@code SplibValidationHelper} already prefixes it with the containing field name.
   *     {@code toItemPropertyPath} lets each caller supply whatever conversion (if any) makes its
   *     {@code ConstraintViolation} paths comparable 
   *     to its own {@code BusinessViolation} paths.</p>
   *
   * @param violations violations collected so far
   * @param toItemPropertyPath converts a {@code ConstraintViolation}'s fully qualified
   *     {@code propertyPath} into the same shape as the {@code BusinessViolation} item property
   *     paths raised by the caller
   * @return a new {@code Violations} with the masked constraint violations removed
   */
  public static Violations excludeConstraintViolationsMaskedByRequiredError(Violations violations,
      Function<String, String> toItemPropertyPath) {
    Set<@NonNull String> requiredItemPropertyPaths = violations.getBusinessViolations().stream()
        .filter(bv -> bv.getMessageId().equals(SplibUiConstants.MESSAGE_KEY_NOT_EMPTY))
        .flatMap(bv -> Arrays.stream(bv.getItemPropertyPaths())).collect(Collectors.toSet());

    Violations filtered = new Violations().messageParameters(violations.messageParameters());
    violations.getConstraintViolations().stream()
        .filter(cv -> !requiredItemPropertyPaths
            .contains(toItemPropertyPath.apply(cv.getPropertyPath().toString())))
        .forEach(filtered::add);
    violations.getBusinessViolations().forEach(filtered::add);
    return filtered;
  }
}
