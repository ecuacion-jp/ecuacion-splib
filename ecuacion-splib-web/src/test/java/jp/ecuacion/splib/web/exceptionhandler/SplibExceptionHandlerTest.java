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
package jp.ecuacion.splib.web.exceptionhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.item.Item;
import jp.ecuacion.lib.core.item.ItemContainer;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.validation.constraints.AnyNotNull;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

/**
 * Unit tests for {@link SplibExceptionHandler#addViolationErrorsTo}.
 *
 * <p>These tests exercise the core violation-to-BindingResult mapping logic
 * independently of Spring MVC / Servlet infrastructure.</p>
 *
 * <p>The test locale is fixed to {@code Locale.ROOT} so that
 * {@code messages_splib-web-test.properties} (no locale suffix) is resolved.</p>
 */
@ExtendWith(MockitoExtension.class)
class SplibExceptionHandlerTest {

  // message IDs defined in messages_splib-web-test.properties
  private static final String MSG1 = "jp.ecuacion.splib.web.test.violation1";
  private static final String MSG2 = "jp.ecuacion.splib.web.test.violation2";

  @SuppressWarnings("null")
  @Mock
  private HttpServletRequest request;

  @SuppressWarnings("null")
  @Mock
  private SplibLoginStateUtil loginStateUtil;

  /** Concrete subclass for testing only; request-related methods are never invoked. */
  @SuppressWarnings("null")
  private SplibExceptionHandler handler;

  // =========================================================================
  // Test support classes
  // =========================================================================

  /**
   * Test bean for generating a field-level {@code ConstraintViolation}.
   *
   * <p>Leaving the {@code name} field {@code null} triggers a {@code @NotNull} violation,
   * which produces a {@code ConstraintViolation} with {@code propertyPath = "name"}.</p>
   */
  private static class CvBean {
    @NotNull
    @SuppressWarnings({"UnusedVariable", "MultipleNullnessAnnotations"})
    @Nullable
    String name; // null by default → triggers @NotNull violation
  }

  /**
   * Test bean for generating a {@code ConstraintViolation} with {@code propertyPath = "email"}.
   *
   * <p>{@code TestRecord} only has a {@code name} field; {@code email} does not exist.
   * When the CV produced from this bean is passed to a {@code BindingResult} targeting
   * {@code TestForm}, {@code qualifyForForm} cannot resolve {@code "email"} and
   * auto-fallback to a global error occurs.</p>
   */
  private static class CvBeanEmail {
    @NotNull
    @SuppressWarnings({"UnusedVariable", "MultipleNullnessAnnotations"})
    @Nullable
    String email; // null by default → triggers @NotNull violation
  }

  /**
   * Test record implementing {@code SplibRecord} and {@code ItemContainer}.
   *
   * <p>Holds a {@code name} field so that the item property path {@code "name"}
   * can be resolved as {@code "testRecord.name"} via {@code qualifyForForm}.</p>
   */
  private static class TestRecord extends SplibRecord implements ItemContainer {
    @SuppressWarnings({"unused"})
    @Nullable
    String name;

    @Override
    public Item[] customizedItems() {
      return new Item[] {};
    }
  }

  /**
   * Test form subclassing {@code SplibGeneralForm}.
   *
   * <p>Holds a {@code testRecord} field. The path {@code "name"} resolves to
   * {@code "testRecord.name"}, while a path such as {@code "nonExistent"} cannot be
   * resolved in any record and falls back to a global error.</p>
   */
  private static class TestForm extends SplibGeneralForm {
    @SuppressWarnings("unused")
    TestRecord testRecord = new TestRecord();
  }

  /**
   * Test bean for {@code ClassValidator}-based constraint ({@code @AnyNotNull}).
   *
   * <p>Leaving {@code name} as {@code null} triggers an {@code @AnyNotNull} failure.
   * The validator extends {@code ClassValidator}, so {@code isClassValidatorConstraint}
   * returns {@code true} and field paths are resolved from the annotation's
   * {@code propertyPath} attribute rather than from {@code cv.getPropertyPath()}.</p>
   */
  @AnyNotNull(propertyPath = {"name"})
  private static class AnyNotNullBean {
    @SuppressWarnings("unused")
    @Nullable
    String name; // null by default → AnyNotNull fails (no non-null values)
  }

  /**
   * Test bean for ClassValidator with a propertyPath that does NOT exist in {@code TestForm}.
   *
   * <p>{@code TestRecord} only has {@code name}; {@code email} is not present.
   * When validated against a {@code TestForm}-backed {@code BindingResult},
   * the path cannot be resolved and the error should fall back to a global error.</p>
   */
  @AnyNotNull(propertyPath = {"email"})
  private static class AnyNotNullBeanWithEmail {
    @SuppressWarnings("unused")
    @Nullable
    String email; // null by default → AnyNotNull fails (no non-null values)
  }

  /**
   * Test record that holds a single nested {@code @Valid AnyNotNullBean} field.
   *
   * <p>Validating this record cascades into {@code nestedBean}. Because
   * {@code AnyNotNullBean.name} is {@code null}, the {@code @AnyNotNull} class-level
   * constraint fails and produces a {@code ConstraintViolation} with
   * {@code propertyPath = "nestedBean"} (non-empty).</p>
   */
  private static class TestRecordWithNested extends SplibRecord implements ItemContainer {
    @SuppressWarnings("UnusedVariable")
    @Valid
    AnyNotNullBean nestedBean = new AnyNotNullBean();

    @Override
    public Item[] customizedItems() {
      return new Item[] {};
    }
  }

  /**
   * Test form whose record contains a nested bean.
   *
   * <p>The path {@code "nestedBean.name"} resolves to {@code "testRecord.nestedBean.name"}
   * via {@code qualifyForForm}, whereas any path that does not correspond to a field in
   * {@code TestRecordWithNested} cannot be resolved and falls back to a global error.</p>
   */
  private static class TestFormWithNested extends SplibGeneralForm {
    @SuppressWarnings("unused")
    TestRecordWithNested testRecord = new TestRecordWithNested();
  }

  /**
   * Test record that holds a {@code List} of nested {@code @Valid AnyNotNullBean} elements.
   *
   * <p>Validating this record cascades into each list element. Because
   * {@code AnyNotNullBean.name} is {@code null}, the class-level constraint on
   * {@code nestedList[0]} fails with {@code propertyPath = "nestedList[0]"}.</p>
   */
  private static class TestRecordWithNestedList extends SplibRecord implements ItemContainer {
    @SuppressWarnings("UnusedVariable")
    @Valid
    @Nullable List<@Nullable AnyNotNullBean> nestedList = List.of(new AnyNotNullBean());

    @Override
    public Item[] customizedItems() {
      return new Item[] {};
    }
  }

  /**
   * Test form whose record contains a list of nested beans.
   *
   * <p>The path {@code "nestedList[0].name"} resolves to
   * {@code "testRecord.nestedList[0].name"} via {@code qualifyForForm}.</p>
   */
  private static class TestFormWithNestedList extends SplibGeneralForm {
    @SuppressWarnings("unused")
    TestRecordWithNestedList testRecord = new TestRecordWithNestedList();
  }

  /**
   * Class-level constraint annotation that always fails validation.
   *
   * <p>Applying this annotation to a bean and validating it produces a
   * {@code ConstraintViolation} whose {@code propertyPath} is the empty string
   * ({@code ""}).</p>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Constraint(validatedBy = AlwaysFailClassLevelValidator.class)
  @interface AlwaysFailClassLevel {
    String message() default "jp.ecuacion.splib.web.test.violation1";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
  }

  /** Validator for {@link AlwaysFailClassLevel}; always returns {@code false}. */
  public static class AlwaysFailClassLevelValidator
      implements ConstraintValidator<AlwaysFailClassLevel, Object> {
    @Override
    public boolean isValid(@Nullable Object value, @Nullable ConstraintValidatorContext context) {
      return false;
    }
  }

  /**
   * Test bean annotated with {@link AlwaysFailClassLevel}.
   *
   * <p>Validating this bean produces a {@code ConstraintViolation}
   * with {@code propertyPath = ""}.</p>
   */
  @AlwaysFailClassLevel
  private static class ClassLevelBean {}

  // =========================================================================
  // Setup
  // =========================================================================

  @BeforeEach
  void setUp() {
    handler = new SplibExceptionHandler(request, null, loginStateUtil) {};
  }

  /**
   * Creates a simple {@code BindingResult} backed by a plain {@code Object}.
   *
   * <p>Using a plain {@code Object} as the target bypasses the form-qualify logic
   * in {@code qualifyItemPropertyPaths}, so item property paths are used as-is.</p>
   */
  private BindingResult newBindingResult() {
    return new BeanPropertyBindingResult(new Object(), "testBean");
  }

  private ViolationException violationOf(BusinessViolation... bvs) {
    Violations violations = new Violations();
    for (BusinessViolation bv : bvs) {
      violations.add(bv);
    }
    return new ViolationException(violations);
  }

  // =========================================================================
  // atItem=false, atTop=false → configuration error
  // =========================================================================

  @Test
  void bothFalse_throwsRuntimeException() {
    ViolationException ex = violationOf(new BusinessViolation(MSG1));
    BindingResult br = newBindingResult();

    assertThatThrownBy(() -> handler.addViolationErrorsTo(ex, br, false, false, Locale.ROOT))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("shown-at-each-item");
  }

  // =========================================================================
  // BusinessViolation: no itemPropertyPath
  // =========================================================================

  @Nested
  class BusinessViolation_NoPath {

    @Test
    void atTop_only__global1_field0() {
      // No itemPropertyPath → forced fallback to at-top
      ViolationException ex = violationOf(new BusinessViolation(MSG1));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, false, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrorCount()).isEqualTo(0);
    }

    @Test
    void atItem_only__global1_field0() {
      // No itemPropertyPath → cannot attach to a field, falls back to at-top
      ViolationException ex = violationOf(new BusinessViolation(MSG1));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      // Even with atItem=true, an empty path falls back to top
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrorCount()).isEqualTo(0);
    }

    @Test
    void atBoth__global1_field0_noSummary() {
      // atEachItemAdded=false, so no summary row is added → global=1
      ViolationException ex = violationOf(new BusinessViolation(MSG1));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, true, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrorCount()).isEqualTo(0);
    }
  }

  // =========================================================================
  // BusinessViolation: with itemPropertyPath (single path)
  // =========================================================================

  @Nested
  class BusinessViolation_WithPath {

    @Test
    void atTop_only__global1_field1_noSummary() {
      // atTop=true, atItem=false → field error IS registered (for is-invalid styling),
      // but atEachItemAdded=false so the summary is not added.
      ViolationException ex =
          violationOf(new BusinessViolation(new String[] {"name"}, MSG1));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, false, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("name");
    }

    @Test
    void atItem_only__field1_global1_withSummary() {
      // atItem=true, atTop=false → field error + summary
      // The summary is added whenever at least one field error exists,
      // regardless of the atTop setting.
      ViolationException ex =
          violationOf(new BusinessViolation(new String[] {"name"}, MSG1));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void atBoth__field1_global2_includingSummary() {
      // atItem=true, atTop=true → field error + at-top error + summary
      // globalErrorCount = at-top(1) + summary(1) = 2
      ViolationException ex =
          violationOf(new BusinessViolation(new String[] {"name"}, MSG1));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, true, true, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(2);
    }
  }

  // =========================================================================
  // BusinessViolation: SplibGeneralForm target + auto-fallback
  //
  // When the BindingResult targets a SplibGeneralForm, each BV itemPropertyPath
  // is checked against the form's records.
  // - If found, the error is stored under the qualified path ("testRecord.name").
  // - If not found, the error automatically falls back to a global error
  //   to prevent it from being silently lost.
  // =========================================================================

  @Nested
  class BusinessViolation_SplibFormTarget {

    private BindingResult brWithTestForm() {
      return new BeanPropertyBindingResult(new TestForm(), "testForm");
    }

    @Test
    void atItem_pathExists__qualifiedFieldError_withSummary() {
      // path="name" resolves to "testRecord.name"
      // → field error stored under the qualified path + summary
      ViolationException ex = violationOf(new BusinessViolation(new String[] {"name"}, MSG1));
      BindingResult br = brWithTestForm();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("testRecord.name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void atItem_pathNotFound__globalFallback() {
      // path="nonExistent" cannot be resolved in any record of the form
      // → auto-fallback: registered as a global error (prevents silent loss)
      ViolationException ex =
          violationOf(new BusinessViolation(new String[] {"nonExistent"}, MSG1));
      BindingResult br = brWithTestForm();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(0);
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
    }
  }

  // =========================================================================
  // BusinessViolation: multiple itemPropertyPaths
  // =========================================================================

  @Nested
  class BusinessViolation_MultiPath {

    @Test
    void atItem_only__field2_global1_withSummary() {
      // Two paths → two field errors + one summary
      ViolationException ex =
          violationOf(new BusinessViolation(new String[] {"name", "email"}, MSG1));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(2);
      assertThat(br.getFieldErrors().stream().map(e -> e.getField()).toList())
          .containsExactlyInAnyOrder("name", "email");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }
  }

  // =========================================================================
  // Multiple BusinessViolations
  // =========================================================================

  @Nested
  class MultipleBusinessViolations {

    @Test
    void atTop_only__global2_field0() {
      ViolationException ex =
          violationOf(new BusinessViolation(MSG1), new BusinessViolation(MSG2));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, false, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(2);
      assertThat(br.getFieldErrorCount()).isEqualTo(0);
    }

    @Test
    void atItem_only__mixed_paths__global2_field1() {
      // One violation has no path (top fallback), the other has a path (field error)
      ViolationException ex =
          violationOf(new BusinessViolation(MSG1), new BusinessViolation(new String[] {"name"}, MSG2));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      // MSG1: no path → top fallback → global(1)
      // MSG2: has path → field(1)
      // A field error exists, so the summary is added → global(+1)
      assertThat(br.getGlobalErrorCount()).isEqualTo(2); // top fallback(1) + summary(1)
      assertThat(br.getFieldErrorCount()).isEqualTo(1);
    }
  }

  // =========================================================================
  // ConstraintViolation: field-level (propertyPath = "name")
  //
  // Verifies the basic field-level CV behaviour.
  // No special MessageParameters are set (default values apply).
  // =========================================================================

  @Nested
  class ConstraintViolation_FieldLevel {

    private ViolationException cvOf() {
      return new ViolationException(new Violations().validate(new CvBean()));
    }

    @Test
    void atItem_only__field1_global1_withSummary() {
      // @NotNull violation (name=null) → propertyPath="name" → field error + summary
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(cvOf(), br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void atTop_only__global1_field1_noSummary() {
      // atTop=true, atItem=false → field error IS registered (for is-invalid styling),
      // but atEachItemAdded=false so the summary is not added.
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(cvOf(), br, false, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("name");
    }

    @Test
    void atBoth__field1_global2_includingSummary() {
      // atItem=true, atTop=true → field error + global error + summary
      // globalErrorCount = at-top(1) + summary(1) = 2
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(cvOf(), br, true, true, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(2);
    }
  }

  // =========================================================================
  // ConstraintViolation: class-level (propertyPath = "")
  //
  // A class-level ConstraintViolation has an empty propertyPath.toString().
  // Since there is no field to attach the error to, it always falls back to
  // a global error (the CV's already-interpolated message is used directly
  // because ExceptionUtil.getMessageList cannot resolve an item name from
  // an empty path).
  // =========================================================================

  @Nested
  class ConstraintViolation_ClassLevel {

    private ViolationException classLevelCvOf() {
      return new ViolationException(new Violations().validate(new ClassLevelBean()));
    }

    @Test
    void atItem_only__globalFallback_becauseNoField() {
      // propertyPath="" → no field to attach → needsMsgAtTop forced to true
      // → registered as a global error even though atTop=false
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(classLevelCvOf(), br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(0);
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
    }

    @Test
    void atBoth__globalOnly_noSummary() {
      // propertyPath="" → atEachItemAdded=false → no summary
      // global: at-top(1) only
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(classLevelCvOf(), br, true, true, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(0);
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
    }
  }

  // =========================================================================
  // ConstraintViolation: SplibGeneralForm target + auto-fallback
  //
  // When the BindingResult targets a SplibGeneralForm, the CV propertyPath is
  // checked against the form's records. If the path cannot be resolved,
  // the error automatically falls back to a global error (prevents silent loss).
  // This is the CV counterpart of BusinessViolation_SplibFormTarget.atItem_pathNotFound.
  // =========================================================================

  @Nested
  class ConstraintViolation_SplibFormTarget {

    private BindingResult brWithTestForm() {
      return new BeanPropertyBindingResult(new TestForm(), "testForm");
    }

    @Test
    void atItem_pathFound__qualifiedFieldError_withSummary() {
      // @NotNull violation (name=null) → propertyPath="name"
      // TestRecord has "name" → qualifyForForm(testForm, "name") = "testRecord.name"
      // → field error stored under the qualified path + summary
      ViolationException ex = new ViolationException(new Violations().validate(new CvBean()));
      BindingResult br = brWithTestForm();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("testRecord.name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void atItem_pathNotFound__globalFallback() {
      // Validate CvBeanEmail ("email") → propertyPath="email"
      // TestRecord only has "name"; "email" does not exist
      // → qualifyForForm(testForm, "email") = null → auto-fallback: global error
      ViolationException ex =
          new ViolationException(new Violations().validate(new CvBeanEmail()));
      BindingResult br = brWithTestForm();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(0);
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
    }
  }

  // =========================================================================
  // ConstraintViolation: ClassValidator-based (@AnyNotNull)
  //
  // Constraints whose validator extends ClassValidator have
  // isClassValidatorConstraint=true. Field paths are resolved from the
  // annotation's propertyPath attribute rather than from cv.getPropertyPath().
  // - Plain Object target: annotation paths are used as-is.
  // - SplibGeneralForm target: paths are qualified through qualifyItemPropertyPaths.
  //
  // This differs from @NotNull (a standard ConstraintValidator), where the
  // validator itself carries the field name in cv.getPropertyPath().
  // =========================================================================

  @Nested
  class ConstraintViolation_ClassValidatorBased {

    private ViolationException anyNotNullCvOf() {
      return new ViolationException(new Violations().validate(new AnyNotNullBean()));
    }

    @Test
    void plainTarget_atItem_only__fieldAtAnnotationPath_withSummary() {
      // @AnyNotNull(propertyPath={"name"}) → isClassValidatorConstraint=true
      // beanPath="", annotationPaths=["name"]
      // target=Object → qualifyItemPropertyPaths returns ["name"] unchanged
      // atItem=true → field error at "name" + summary
      BindingResult br = newBindingResult(); // target=Object

      handler.addViolationErrorsTo(anyNotNullCvOf(), br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void splibFormTarget_atItem_only__qualifiesPath_withSummary() {
      // @AnyNotNull(propertyPath={"name"}) → isClassValidatorConstraint=true
      // beanPath="", annotationPaths=["name"]
      // target=TestForm → qualifyForForm → "testRecord.name"
      // atItem=true → field error at "testRecord.name" + summary
      BindingResult br = new BeanPropertyBindingResult(new TestForm(), "testForm");

      handler.addViolationErrorsTo(anyNotNullCvOf(), br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("testRecord.name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void splibFormTarget_pathNotFound__globalFallback() {
      // @AnyNotNull(propertyPath={"email"}) → isClassValidatorConstraint=true
      // beanPath="", annotationPaths=["email"]
      // target=TestForm → qualifyForForm("email") = null (TestRecord has no "email" field)
      // → anyPathNotFound=true → needsMsgAtTop forced true → global error (no silent loss)
      ViolationException ex =
          new ViolationException(new Violations().validate(new AnyNotNullBeanWithEmail()));
      BindingResult br = new BeanPropertyBindingResult(new TestForm(), "testForm");

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(0);
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
    }
  }

  // =========================================================================
  // Mixed CV and BV
  //
  // Verifies that when both CV and BV exist in the same ViolationException,
  // each loop processes them independently but the atEachItemErrorAdded flag
  // and the summary are aggregated correctly.
  // =========================================================================

  @Nested
  class MixedCvAndBv {

    /**
     * Builds a {@code ViolationException} holding both a CV and a BV.
     *
     * <p>CV has {@code propertyPath = "name"}; BV has {@code itemPropertyPath = "email"}.</p>
     */
    private ViolationException mixedEx() {
      Violations violations = new Violations()
          .validate(new CvBean())                                     // CV: "name"
          .add(new BusinessViolation(new String[] {"email"}, MSG1));  // BV: "email"
      return new ViolationException(violations);
    }

    @Test
    void atItem_only__field2_global1_withSummary() {
      // CV("name") + BV("email") → both registered as field errors + summary
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(mixedEx(), br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(2);
      assertThat(br.getFieldErrors().stream().map(e -> e.getField()).toList())
          .containsExactlyInAnyOrder("name", "email");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void atBoth__field2_global3_includingSummary() {
      // CV("name") + BV("email") processed with atBoth
      // field:  CV(1) + BV(1)                        = 2
      // global: CV at-top(1) + BV at-top(1) + summary(1) = 3
      // Even though both CV and BV contribute to atEachItemErrorAdded,
      // the summary is added only once.
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(mixedEx(), br, true, true, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(2);
      assertThat(br.getFieldErrors().stream().map(e -> e.getField()).toList())
          .containsExactlyInAnyOrder("name", "email");
      assertThat(br.getGlobalErrorCount()).isEqualTo(3);
    }
  }

  // =========================================================================
  // ConstraintViolation: nested @Valid bean with ClassValidator-based constraint
  //
  // When a class-level ClassValidator constraint fires on a nested @Valid bean,
  // cv.getPropertyPath() is non-empty (e.g., "nestedBean" or "nestedList[0]").
  // This is the else-branch of the beanPath.isEmpty() check.
  //
  // Before the fix, the else branch skipped qualifyForForm entirely:
  //   - record field prefix was never prepended (wrong path registered)
  //   - anyPathNotFound was never set (top fallback never fired)
  //   → result: FieldError registered at a path that no th:errors can bind,
  //     but the summary "messagesLinkedToItemsExist" was still shown.
  //
  // After the fix, the else branch also calls qualifyForForm(form, beanPath+"."+path):
  //   - path found  → qualified path ("testRecord.nestedBean.name") used for FieldError
  //   - path not found → anyPathNotFound=true → top fallback fires (no silent loss)
  // =========================================================================

  @Nested
  class ConstraintViolation_NestedClassValidatorBased {

    @Test
    void nestedBean_splibFormTarget_pathFound__qualifiedFieldError_withSummary() {
      // TestRecordWithNested has @Valid AnyNotNullBean nestedBean (name=null → always fails)
      // validate(TestRecordWithNested) → CV: propertyPath="nestedBean", annotationPaths=["name"]
      // beanPath="nestedBean" (non-empty → else branch)
      // qualifyForForm(TestFormWithNested, "nestedBean.name") → "testRecord.nestedBean.name"
      ViolationException ex =
          new ViolationException(new Violations().validate(new TestRecordWithNested()));
      BindingResult br =
          new BeanPropertyBindingResult(new TestFormWithNested(), "testFormWithNested");

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("testRecord.nestedBean.name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void nestedBean_splibFormTarget_pathNotFound__globalFallback() {
      // CV: propertyPath="nestedBean", annotationPaths=["name"]  (from TestRecordWithNested)
      // Target: TestForm (record=TestRecord, which has only "name", no "nestedBean" field)
      // qualifyForForm(TestForm, "nestedBean.name") → null (TestRecord has no nestedBean)
      // → anyPathNotFound=true → needsMsgAtTop forced true → global error (top fallback)
      ViolationException ex =
          new ViolationException(new Violations().validate(new TestRecordWithNested()));
      BindingResult br = new BeanPropertyBindingResult(new TestForm(), "testForm");

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(0);
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
    }

    @Test
    void nestedList_splibFormTarget_pathFound__qualifiedFieldError_withSummary() {
      // TestRecordWithNestedList has @Valid List<AnyNotNullBean> nestedList
      // nestedList[0].name=null → CV: propertyPath="nestedList[0]", annotationPaths=["name"]
      // beanPath="nestedList[0]" (non-empty → else branch)
      // qualifyForForm(TestFormWithNestedList, "nestedList[0].name")
      //   → "testRecord.nestedList[0].name"
      // This is the exact scenario from the bug report.
      ViolationException ex =
          new ViolationException(new Violations().validate(new TestRecordWithNestedList()));
      BindingResult br =
          new BeanPropertyBindingResult(new TestFormWithNestedList(), "testFormWithNestedList");

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("testRecord.nestedList[0].name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }
  }
}
