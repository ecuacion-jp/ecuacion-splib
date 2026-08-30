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

import static org.assertj.core.api.Assertions.assertThat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.function.Function;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.ui.constant.SplibUiConstants;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for
 * {@link SplibViolationUtil#excludeConstraintViolationsMaskedByRequiredError}.
 */
class SplibViolationUtilTest {

  /** Prefix a real caller (e.g. {@code SplibGeneralForm#toItemPropertyPath}) would strip off a
   *  {@code ConstraintViolation}'s fully qualified propertyPath before comparing it against
   *  {@code BusinessViolation} item property paths. */
  private static final Function<String, String> STRIP_TEST_RECORD_PREFIX = path -> path
      .startsWith("testRecord.") ? path.substring("testRecord.".length()) : path;

  /**
   * Record with a {@code @NotNull name} field, left {@code null} by default so validating a
   * {@code TestForm} cascades into it and produces a {@code ConstraintViolation} whose
   * {@code propertyPath} is fully qualified as {@code "testRecord.name"}.
   */
  private static class TestRecord {
    @NotNull
    @SuppressWarnings({"UnusedVariable", "MultipleNullnessAnnotations"})
    @Nullable
    String name;
  }

  /**
   * Test form whose record field is {@code @Valid}-cascaded, matching how a real
   * {@code SplibEditRecForm}'s root record field is declared.
   */
  private static class TestForm {
    @Valid
    TestRecord testRecord = new TestRecord();
  }

  @Test
  void constraintViolationOnRequiredField_isRemoved() {
    // CV: "testRecord.name" (fully qualified, via @Valid cascade)
    // BV: itemPropertyPath = "name" (as SplibGeneralForm#validateNotEmpty actually produces
    // it) -> both refer to the same field once "testRecord." is stripped from the CV's path.
    Violations violations = new Violations().validate(new TestForm())
        .add(new BusinessViolation(new String[] {"name"},
            SplibUiConstants.MESSAGE_KEY_NOT_EMPTY));

    Violations filtered = SplibViolationUtil
        .excludeConstraintViolationsMaskedByRequiredError(violations, STRIP_TEST_RECORD_PREFIX);

    assertThat(filtered.getConstraintViolations()).isEmpty();
    assertThat(filtered.getBusinessViolations()).hasSize(1);
  }

  @Test
  void constraintViolationOnUnrelatedField_isKept() {
    // BV targets "otherField", unrelated to the CV's "name" -> nothing is removed.
    Violations violations = new Violations().validate(new TestForm())
        .add(new BusinessViolation(new String[] {"otherField"},
            SplibUiConstants.MESSAGE_KEY_NOT_EMPTY));

    Violations filtered = SplibViolationUtil
        .excludeConstraintViolationsMaskedByRequiredError(violations, STRIP_TEST_RECORD_PREFIX);

    assertThat(filtered.getConstraintViolations().stream()
        .map(cv -> cv.getPropertyPath().toString())).containsExactly("testRecord.name");
  }

  @Test
  void businessViolationWithUnrelatedMessageId_doesNotMaskConstraintViolations() {
    // A BV that isn't the required-field check (different messageId) must not mask anything,
    // even though it targets the same field as the CV.
    Violations violations = new Violations().validate(new TestForm())
        .add(new BusinessViolation(new String[] {"name"}, "some.other.message"));

    Violations filtered = SplibViolationUtil
        .excludeConstraintViolationsMaskedByRequiredError(violations, STRIP_TEST_RECORD_PREFIX);

    assertThat(filtered.getConstraintViolations().stream()
        .map(cv -> cv.getPropertyPath().toString())).containsExactly("testRecord.name");
  }

  @Test
  void noBusinessViolations_keepsAllConstraintViolations() {
    Violations violations = new Violations().validate(new TestForm());

    Violations filtered = SplibViolationUtil
        .excludeConstraintViolationsMaskedByRequiredError(violations, STRIP_TEST_RECORD_PREFIX);

    assertThat(filtered.getConstraintViolations()).hasSize(1);
    assertThat(filtered.getBusinessViolations()).isEmpty();
  }

  @Test
  void identityToItemPropertyPath_matchesAlreadyQualifiedPathsDirectly() {
    // Mirrors how SplibValidationHelper#validate(Object) calls this method: its
    // BusinessViolation paths are already field-prefixed the same way ConstraintViolation
    // paths are, so Function.identity() (no conversion) is the correct comparison.
    Violations violations = new Violations().validate(new TestForm())
        .add(new BusinessViolation(new String[] {"testRecord.name"},
            SplibUiConstants.MESSAGE_KEY_NOT_EMPTY));

    Violations filtered = SplibViolationUtil
        .excludeConstraintViolationsMaskedByRequiredError(violations, Function.identity());

    assertThat(filtered.getConstraintViolations()).isEmpty();
  }
}
