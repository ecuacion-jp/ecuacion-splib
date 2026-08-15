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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jp.ecuacion.lib.core.exception.ConstraintViolationExceptionWithParameters;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.exception.ViolationWarningException;
import jp.ecuacion.lib.core.item.Item;
import jp.ecuacion.lib.core.item.ItemContainer;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.util.PropertyPathUtil;
import jp.ecuacion.lib.core.util.internal.PropertiesFileUtilBundleReader;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.core.violation.Violations.MessageParameters;
import jp.ecuacion.lib.validation.constraints.AnyNotNull;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.web.bean.ReturnUrlBuilder;
import jp.ecuacion.splib.web.bean.WarnMessageBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibEditController;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.RedirectException;
import jp.ecuacion.splib.web.form.SplibEditForm;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.service.SplibGeneralService;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

  @BeforeAll
  static void init() {
    PropertiesFileUtilBundleReader.addToDynamicPostfixList("splib-web-test");
  }

  // application.properties values consumed by the handler methods under test (as opposed to
  // addViolationErrorsTo, which receives needsMsgAtItem / needsMsgAtTop as explicit arguments
  // and never reads these). Registered once via PropertiesFileUtil#setApplicationResolver
  // because RedirectToHomePageException reads "home-page" in a static initializer, so it must
  // be available before that class is first loaded by any test.
  private static final Map<String, String> APPLICATION_PROPS = Map.of(
      "jp.ecuacion.splib.web.home-page", "/top",
      SplibExceptionHandler.PROP_KEY_SHOWN_AT_EACH_ITEM, "true",
      SplibExceptionHandler.PROP_KEY_SHOWN_AT_THE_TOP, "true");

  @BeforeAll
  static void initApplicationProps() {
    PropertiesFileUtil.setApplicationResolver(APPLICATION_PROPS::get);
  }

  @AfterAll
  static void clearApplicationProps() {
    PropertiesFileUtil.setApplicationResolver(null);
  }

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
   * {@code SplibRecord} carrying a {@code ClassValidator}-based constraint directly on itself
   * (rather than on a further-nested plain bean), analogous to the real
   * {@code @NotEmptyWhen(propertyPath = {"awsAccessKeyId", "awsSecretAccessKey"}, ...)} on
   * {@code CloudServiceEditRecord}.
   */
  @AnyNotNull(propertyPath = {"name"})
  private static class AnyNotNullRecord extends SplibRecord implements ItemContainer {
    @SuppressWarnings({"UnusedVariable", "unused"})
    @Nullable
    String name; // null by default → AnyNotNull fails (no non-null values)

    @Override
    public Item[] customizedItems() {
      return new Item[] {};
    }
  }

  /**
   * Test form whose {@code @Valid}-cascaded record field is directly (non-generically) typed,
   * matching how a real {@code CloudServiceEditForm.cloudService} field is declared.
   *
   * <p>Validating this form directly cascades into {@code rec}, whose class-level
   * {@code @AnyNotNull} constraint then fails. The resulting {@code ConstraintViolation}'s
   * {@code propertyPath} ({@code beanPath} in {@code addConstraintViolation}) is already fully
   * qualified as {@code "rec"} - combined with the annotation's {@code propertyPath} attribute
   * ({@code "name"}), the target path is {@code "rec.name"}, already fully qualified - unlike
   * {@link AnyNotNullBean}, which is validated standalone and yields an empty {@code beanPath}.</p>
   */
  private static class TestFormWithDirectClassValidatorRecord extends SplibGeneralForm {
    @SuppressWarnings("UnusedVariable")
    @Valid
    AnyNotNullRecord rec = new AnyNotNullRecord();
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
    @Nullable
    List<@Nullable AnyNotNullBean> nestedList = List.of(new AnyNotNullBean());

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
   * Plain (non-ClassValidator) nested bean, analogous to {@code Acc} nested under
   * {@code AccGeneralRecord} in a real application.
   *
   * <p>Leaving {@code mailAddress} {@code null} triggers a plain {@code @NotNull} failure.</p>
   */
  private static class NestedPlainBean {
    @NotNull
    @SuppressWarnings({"UnusedVariable", "MultipleNullnessAnnotations"})
    @Nullable
    String mailAddress; // null by default → triggers @NotNull violation
  }

  /**
   * Test record holding a {@code @Valid}-cascaded plain nested bean.
   */
  private static class TestRecordWithValidNested extends SplibRecord implements ItemContainer {
    @SuppressWarnings("UnusedVariable")
    @Valid
    NestedPlainBean acc = new NestedPlainBean();

    @Override
    public Item[] customizedItems() {
      return new Item[] {};
    }
  }

  /**
   * Generic base form, mirroring the real {@code SplibEditRecForm<R extends SplibRecord>}.
   *
   * <p>Declaring {@code rec} with a generic type parameter (rather than a concrete record
   *     type directly) matters: due to type erasure, {@code rec}'s <em>declared</em> type as
   *     seen via reflection on the {@code Field} object is the erased bound ({@code
   *     SplibRecord}), not the concrete runtime subtype. A path-resolution approach that walks
   *     declared field types from {@code form.getClass()} (like {@link PropertyPathUtil#getClass})
   *     cannot see past {@code rec} into record-specific fields such as {@code acc}; only
   *     walking the actual runtime object graph (like {@link PropertyPathUtil#getValue}) can.</p>
   */
  private abstract static class GenericRecForm<R extends SplibRecord> extends SplibGeneralForm {
    @SuppressWarnings("UnusedVariable")
    @Valid
    @Nullable
    R rec;
  }

  /**
   * Test form whose record field is itself {@code @Valid}-cascaded, matching how a real
   * {@code SplibEditRecForm.rec} field is annotated - including the generic declaration
   * (see {@link GenericRecForm}).
   *
   * <p>Validating this form directly (as {@code SplibControllerPrepareHelper#validateForm}
   * and {@code SplibGeneralForm#validate} always do in production) cascades through
   * {@code rec} and then {@code acc}, so the resulting {@code ConstraintViolation}'s
   * {@code propertyPath} arrives already fully qualified as {@code "rec.acc.mailAddress"} -
   * unlike {@link CvBean}, which is validated standalone and yields a record-relative path.</p>
   */
  private static class TestFormWithValidNested extends GenericRecForm<TestRecordWithValidNested> {
    TestFormWithValidNested() {
      rec = new TestRecordWithValidNested();
    }
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
  private static class ClassLevelBean {
  }

  // =========================================================================
  // Support classes for handler-level tests (handleWarning, handleViolationException, etc.)
  //
  // Unlike addViolationErrorsTo, these methods go through Model / RedirectAttributes /
  // SplibGeneralController, so a minimal but real (non-mocked) controller + service pair is
  // used, matching the "extracted to be independently testable" pattern used elsewhere:
  // Model uses the real ExtendedModelMap, RedirectAttributes uses the real
  // RedirectAttributesModelMap, and only the request (locale / header / model attribute) is
  // mocked, since HttpServletRequest cannot be instantiated directly.
  // =========================================================================

  /** Records the arguments {@code prepareFormForReturn} passes through to the service. */
  private static class TestService extends SplibGeneralService {
    @Nullable
    List<SplibGeneralForm> preparedForms;
    @SuppressWarnings("unused")
    @Nullable
    UserDetails preparedLoginUser;

    @Override
    public void prepareForm(List<SplibGeneralForm> allFormList,
        @Nullable UserDetails loginUser) {
      this.preparedForms = allFormList;
      this.preparedLoginUser = loginUser;
    }
  }

  /**
   * Minimal concrete {@code SplibGeneralController}, analogous to a plain (non-edit) page
   * controller. {@code getService()} is overridden because the real implementation resolves
   * the service from an {@code @Autowired} list that is never populated outside a Spring
   * context.
   */
  private static class TestController extends SplibGeneralController<SplibGeneralService> {
    private final TestService service;

    TestController(String function, TestService service) {
      super(function);
      this.service = service;
    }

    @Override
    public SplibGeneralService getService() {
      return service;
    }

    void setRedirectUrlOnAppException(@Nullable ReturnUrlBuilder builder) {
      this.redirectUrlOnAppException = builder;
    }
  }

  /** Empty edit form; only the type is needed to satisfy {@code SplibEditController}'s bound. */
  private static class TestEditForm extends SplibEditForm {
  }

  /**
   * Minimal concrete {@code SplibEditController}, used only for the
   * {@code instanceof SplibEditController} branch of
   * {@code handleOptimisticLockingFailureException}, which never calls {@code getService()}.
   */
  private static class TestEditController
      extends SplibEditController<TestEditForm, jp.ecuacion.splib.web.service.SplibEditService<TestEditForm>> {
    TestEditController(String function) {
      super(PageTemplatePatternEnum.SINGLE, function);
    }
  }

  /**
   * Builds a {@code Model} pre-populated the way {@code SplibGeneralController#prepare} would
   * have left it before the exception was thrown: {@code form} registered under
   * {@link SplibWebConstants#KEY_FORMS}, {@code controller} under
   * {@link SplibWebConstants#KEY_CONTROLLER}, and a {@code BindingResult} targeting
   * {@code form} registered under its {@code BindingResult.MODEL_KEY_PREFIX + formName} key.
   */
  private Model modelWithForm(SplibGeneralForm form, SplibGeneralController<?> controller) {
    Model model = new ExtendedModelMap();
    model.addAttribute(SplibWebConstants.KEY_FORMS, new SplibGeneralForm[] {form});
    model.addAttribute(SplibWebConstants.KEY_CONTROLLER, controller);
    String formName = StringUtils.uncapitalize(form.getClass().getSimpleName());
    model.addAttribute(BindingResult.MODEL_KEY_PREFIX + formName,
        new BeanPropertyBindingResult(form, formName));
    return model;
  }

  /** Stubs {@code request.getAttribute(KEY_MODEL)} to return {@code model}. */
  private void stubModel(@Nullable Model model) {
    when(request.getAttribute(SplibWebConstants.KEY_MODEL)).thenReturn(model);
  }

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
        .isInstanceOf(RuntimeException.class).hasMessageContaining("shown-at-each-item");
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
      ViolationException ex = violationOf(new BusinessViolation(new String[] {"name"}, MSG1));
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
      ViolationException ex = violationOf(new BusinessViolation(new String[] {"name"}, MSG1));
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
      ViolationException ex = violationOf(new BusinessViolation(new String[] {"name"}, MSG1));
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
  // to prevent it from being silently lost.
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
      ViolationException ex = violationOf(new BusinessViolation(MSG1), new BusinessViolation(MSG2));
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(ex, br, false, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(2);
      assertThat(br.getFieldErrorCount()).isEqualTo(0);
    }

    @Test
    void atItem_only__mixed_paths__global2_field1() {
      // One violation has no path (top fallback), the other has a path (field error)
      ViolationException ex = violationOf(new BusinessViolation(MSG1),
          new BusinessViolation(new String[] {"name"}, MSG2));
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
  // Verifies the basic field-level CV behavior.
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
  // "Class-level CV" covers two different patterns, and only one of them
  // actually reaches this early return:
  //
  // - Pattern B (tested here): a constraint whose validator does NOT implement
  //   ClassValidator, annotated directly on a class/record, producing a CV with
  //   propertyPath="". addConstraintViolation's else-branch short-circuits on
  //   pathStr.isEmpty() BEFORE addViolation() is ever called, using the CV's
  //   already-interpolated message as-is (ExceptionUtil.getMessageList cannot
  //   resolve an item name from an empty path - it would throw
  //   RequireNonEmptyException inside Item.<init>). This path is unaffected by
  //   the withItemName fix in addViolation() below.
  // - Pattern A: a constraint whose validator DOES implement ClassValidator
  //   (e.g. @AnyNotNull), even when placed at the class/root level (empty
  //   beanPath). This does NOT short-circuit - annotationPaths is guaranteed
  //   non-empty by MultiplePropertyPathsValidator.initialize(), so it always
  //   reaches addViolation() with a real, nameable target property. See
  //   ConstraintViolation_ClassValidatorBased.splibFormTarget_pathNotFound__globalMessageIncludesItemName
  //   for this pattern.
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

    @Test
    void atTop_only__usesRawConstraintMessage_unaffectedByWithItemNameFix() {
      // Pattern B never reaches addViolation()'s withItemName logic at all - it always
      // uses cv.getMessage() (AlwaysFailClassLevel's message() attribute, a plain string
      // rather than a "{...}" bundle-key reference, so Hibernate Validator's interpolator
      // returns it unchanged) regardless of the withItemName fix.
      BindingResult br = newBindingResult();

      handler.addViolationErrorsTo(classLevelCvOf(), br, false, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getGlobalErrors().get(0).getDefaultMessage())
          .isEqualTo("jp.ecuacion.splib.web.test.violation1");
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
      ViolationException ex = new ViolationException(new Violations().validate(new CvBeanEmail()));
      BindingResult br = brWithTestForm();

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(0);
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
    }

    @Test
    void atTop_pathNotFound__globalMessageIncludesItemName() {
      // Same violation as atItem_pathNotFound__globalFallback, but verifies the actual
      // message TEXT, not just error counts. This reproduces the real bug report shape:
      // a ConstraintViolation validated against an object that isn't part of the current
      // form at all (e.g. ecuacion-util-excel-table validating an Excel-row bean) still
      // has a real, nameable property ("email") - it just has no matching form field.
      // Since there's no field to visually pair the message with, the top message must
      // include the item name to be understandable on its own; withItemName must not be
      // tied to whether a form field happened to be found.
      ViolationException ex = new ViolationException(new Violations().validate(new CvBeanEmail()));
      BindingResult br = brWithTestForm();

      handler.addViolationErrorsTo(ex, br, false, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getGlobalErrors().get(0).getDefaultMessage()).contains("email");
    }
  }

  // =========================================================================
  // ConstraintViolation: SplibGeneralForm target, propertyPath already fully qualified
  //
  // Reproduces the real production shape: the whole form is validated directly
  // (matching SplibControllerPrepareHelper#validateForm / SplibGeneralForm#validate),
  // so a plain constraint failing deep inside a @Valid-cascaded nested bean produces a
  // ConstraintViolation whose propertyPath is already fully qualified from the form root
  // (e.g. "rec.acc.mailAddress"). This must be used as-is (verifyFormPath), not re-resolved
  // as a record-relative path via qualifyForForm (which would fail to find a "rec" field on
  // the record class itself and silently drop the error - the bug this test guards against).
  // =========================================================================

  @Nested
  class ConstraintViolation_SplibFormTarget_FullyQualifiedPath {

    @Test
    void atItem__fieldErrorOnFullyQualifiedPath_withSummary() {
      TestFormWithValidNested form = new TestFormWithValidNested();
      ViolationException ex = new ViolationException(new Violations().validate(form));
      BindingResult br = new BeanPropertyBindingResult(form, "testFormWithValidNested");

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("rec.acc.mailAddress");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
    }

    @Test
    void atTop_only__globalMessageIncludesItemName() {
      // atItem=false, atTop=true: field error is still registered (for is-invalid styling),
      // and since propertyPaths is non-empty, the top message is built withItemName=true -
      // covering the "message lacks item name" half of the original bug report.
      TestFormWithValidNested form = new TestFormWithValidNested();
      ViolationException ex = new ViolationException(new Violations().validate(form));
      BindingResult br = new BeanPropertyBindingResult(form, "testFormWithValidNested");

      handler.addViolationErrorsTo(ex, br, false, true, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("rec.acc.mailAddress");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
    }
  }

  // =========================================================================
  // ConstraintViolation: ClassValidator-based, SplibGeneralForm target,
  // beanPath already fully qualified
  //
  // Reproduces the real production shape for a ClassValidator (e.g. @NotEmptyWhen) declared
  // directly on a @Valid-cascaded, directly (non-generically) typed record field - matching
  // CloudServiceEditForm.cloudService / CloudServiceEditRecord's @NotEmptyWhen. The whole form
  // is validated directly, so beanPath (cv.getPropertyPath()) is already fully qualified from
  // the form root (e.g. "rec"), and beanPath + "." + annotationPath (e.g. "rec.name") must be
  // used as-is (resolveFormPath), not re-resolved as record-relative via qualifyForForm alone.
  // =========================================================================

  @Nested
  class ConstraintViolation_ClassValidatorBased_SplibFormTarget_FullyQualifiedBeanPath {

    @Test
    void atItem__fieldErrorOnFullyQualifiedPath_withSummary() {
      TestFormWithDirectClassValidatorRecord form = new TestFormWithDirectClassValidatorRecord();
      ViolationException ex = new ViolationException(new Violations().validate(form));
      BindingResult br =
          new BeanPropertyBindingResult(form, "testFormWithDirectClassValidatorRecord");

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("rec.name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
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

    @Test
    void splibFormTarget_pathNotFound__globalMessageIncludesItemName() {
      // Pattern A of "class-level CV": AnyNotNullBeanWithEmail is validated standalone, so
      // beanPath (cv.getPropertyPath()) is empty - the constraint fires at the class/root
      // level, exactly like ClassLevelBean in ConstraintViolation_ClassLevel. Unlike that
      // pattern-B case, this DOES reach addViolation() (annotationPaths=["email"] is
      // guaranteed non-empty), and even though "email" doesn't resolve to a TestForm field,
      // the annotation still names a real target property - so the top message must include
      // its item name, same as the plain-CV case above.
      ViolationException ex =
          new ViolationException(new Violations().validate(new AnyNotNullBeanWithEmail()));
      BindingResult br = new BeanPropertyBindingResult(new TestForm(), "testForm");

      handler.addViolationErrorsTo(ex, br, false, true, Locale.ROOT);

      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getGlobalErrors().get(0).getDefaultMessage()).contains("email");
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
      Violations violations = new Violations().validate(new CvBean()) // CV: "name"
          .add(new BusinessViolation(new String[] {"email"}, MSG1)); // BV: "email"
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
      // field: CV(1) + BV(1) = 2
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
  // BusinessViolation: itemPropertyPath is already fully form-qualified
  //
  // Callers that already know the record field name (SplibGeneralForm#validateNotEmpty,
  // SplibValidationHelper) qualify their BusinessViolation's itemPropertyPath up front
  // (e.g. "testRecord.name") rather than leaving it record-relative ("name"). addBusinessViolation
  // must accept that shape directly via resolveFormPath (try as-is first), the same way plain
  // ConstraintViolations are resolved - not go straight to qualifyForForm's record-relative-only
  // resolution, which would fail to find "testRecord.name" as a property of TestRecord itself
  // and incorrectly fall back to a global error.
  // =========================================================================

  @Nested
  class BusinessViolation_AlreadyQualifiedPath {

    @Test
    void atItem__fieldErrorOnAlreadyQualifiedPath_withSummary() {
      ViolationException ex =
          violationOf(new BusinessViolation(new String[] {"testRecord.name"}, MSG1));
      BindingResult br = new BeanPropertyBindingResult(new TestForm(), "testForm");

      handler.addViolationErrorsTo(ex, br, true, false, Locale.ROOT);

      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("testRecord.name");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1); // summary only
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
  // - record field prefix was never prepended (wrong path registered)
  // - anyPathNotFound was never set (top fallback never fired)
  // → result: FieldError registered at a path that no th:errors can bind,
  // but the summary "messagesLinkedToItemsExist" was still shown.
  //
  // After the fix, the else branch also calls qualifyForForm(form, beanPath+"."+path):
  // - path found → qualified path ("testRecord.nestedBean.name") used for FieldError
  // - path not found → anyPathNotFound=true → top fallback fires (no silent loss)
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
      // CV: propertyPath="nestedBean", annotationPaths=["name"] (from TestRecordWithNested)
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
      // → "testRecord.nestedList[0].name"
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

  // =========================================================================
  // handleWarning
  // =========================================================================

  @Nested
  class HandleWarning {

    @SuppressWarnings("null")
    private TestForm form;
    @SuppressWarnings("null")
    private TestController controller;
    @SuppressWarnings("null")
    private Model model;

    @BeforeEach
    void setUpController() {
      form = new TestForm();
      controller = new TestController("testFunc", new TestService());
      model = modelWithForm(form, controller);
      stubModel(model);
      when(request.getLocale()).thenReturn(Locale.ROOT);
    }

    @SuppressWarnings("null")
    @Test
    void webWarning_withButtonId__modelHasWarnMessage_andFormPrepared() {
      Violations violations = new Violations().add(new BusinessViolation(MSG1));
      ViolationWebWarningException ex =
          new ViolationWebWarningException(violations, "btnConfirm");

      ModelAndView mav = handler.handleWarning(ex, null);

      WarnMessageBean bean =
          (WarnMessageBean) model.getAttribute(SplibWebConstants.KEY_WARN_MESSAGE);
      assertThat(bean).isNotNull();
      assertThat(bean.getMessageId()).isEqualTo(MSG1);
      assertThat(bean.getMessage()).isEqualTo("Test violation 1");
      assertThat(bean.getButtonName()).isEqualTo("btnConfirm");

      // prepareFormForReturn delegated to the controller's service.
      TestService service = (TestService) controller.getService();
      assertThat(service.preparedForms).containsExactly(form);

      assertThat(mav.getViewName()).isEqualTo(controller.getDefaultHtmlPageName());
      assertThat(mav.getModel()).isEqualTo(model.asMap());
    }

    @SuppressWarnings("null")
    @Test
    void plainWarning_noButtonId__buttonNameEmpty() {
      Violations violations = new Violations().add(new BusinessViolation(MSG2));
      ViolationWarningException ex = new ViolationWarningException(violations);

      handler.handleWarning(ex, null);

      WarnMessageBean bean =
          (WarnMessageBean) model.getAttribute(SplibWebConstants.KEY_WARN_MESSAGE);
      assertThat(bean).isNotNull();
      assertThat(bean.getMessage()).isEqualTo("Test violation 2");
      // Not a ViolationWebWarningException, so no button id -> getButtonName() falls back to "".
      assertThat(bean.getButtonName()).isEqualTo("");
    }
  }

  // =========================================================================
  // handleViolationException: no SplibGeneralController in the model
  // (plain @Controller / SplibBaseController - dispatches to
  // handleViolationExceptionWithoutController)
  // =========================================================================

  @Nested
  class HandleViolationException_NoController {

    private final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    @BeforeEach
    void setUpNoController() {
      // getController() reads request.getAttribute(KEY_MODEL); leaving it unstubbed makes it
      // return null, exactly like NoResourceFoundException firing before prepare() ran.
      when(request.getLocale()).thenReturn(Locale.ROOT);
    }

    @Test
    void mixedCvAndBv_validReferer__globalErrorsFlashed_redirectsToRefererPath() {
      when(request.getHeader("Referer")).thenReturn("https://example.com/prior/page?x=1");

      Violations violations =
          new Violations().validate(new CvBean()).add(new BusinessViolation(MSG1));
      ViolationException ex = new ViolationException(violations);

      ModelAndView mav = handler.handleViolationException(ex, null, redirectAttributes);

      assertThat(mav.getViewName()).isEqualTo("redirect:/prior/page?x=1");
      @SuppressWarnings("unchecked")
      List<String> errors = (List<String>) redirectAttributes.getFlashAttributes()
          .get(SplibWebConstants.KEY_GLOBAL_ERRORS);
      assertThat(errors).hasSize(2); // one from the CV, one from the BV
    }

    @Test
    void invalidReferer__fallsBackToRoot() {
      // Malformed URI (unterminated IPv6 host literal) -> URI.create throws
      // IllegalArgumentException, caught internally -> redirectTarget stays "/".
      when(request.getHeader("Referer")).thenReturn("http://[not-a-valid-host/x");

      ViolationException ex = violationOf(new BusinessViolation(MSG1));
      ModelAndView mav = handler.handleViolationException(ex, null, redirectAttributes);

      assertThat(mav.getViewName()).isEqualTo("redirect:/");
    }

    @Test
    void noRefererHeader__fallsBackToRoot() {
      when(request.getHeader("Referer")).thenReturn(null);

      ViolationException ex = violationOf(new BusinessViolation(MSG1));
      ModelAndView mav = handler.handleViolationException(ex, null, redirectAttributes);

      assertThat(mav.getViewName()).isEqualTo("redirect:/");
    }
  }

  // =========================================================================
  // handleViolationException: SplibGeneralController present in the model
  // (dispatches to handleViolationExceptionWithController)
  // =========================================================================

  @Nested
  class HandleViolationException_WithController {

    @SuppressWarnings("null")
    private TestController controller;
    @SuppressWarnings("null")
    private TestService service;
    private final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    @BeforeEach
    void setUpController() {
      service = new TestService();
      controller = new TestController("testFunc", service);
      when(request.getLocale()).thenReturn(Locale.ROOT);
    }

    @SuppressWarnings("null")
    @Test
    void fieldLevelCv__fieldErrorFlashed_formPrepared_defaultAbnormalEndRedirect() {
      when(loginStateUtil.getLoginState()).thenReturn("account");

      TestForm form = new TestForm();
      Model model = modelWithForm(form, controller);
      stubModel(model);

      ViolationException ex = new ViolationException(new Violations().validate(new CvBean()));
      ModelAndView mav = handler.handleViolationException(ex, null, redirectAttributes);

      // addViolationErrorsTo delegation: field error resolved against the form's record.
      BindingResult br = (BindingResult) model
          .getAttribute(BindingResult.MODEL_KEY_PREFIX + "testForm");
      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("testRecord.name");

      // prepareFormForReturn delegation.
      assertThat(service.preparedForms).containsExactly(form);

      // Field errors are snapshotted to flash so they survive the redirect's form re-binding.
      @SuppressWarnings("unchecked")
      Map<String, List<?>> fieldErrorsSnapshot = (Map<String, List<?>>) redirectAttributes
          .getFlashAttributes().get(SplibWebConstants.KEY_FLASH_FIELD_ERRORS);
      assertThat(fieldErrorsSnapshot).containsKey(BindingResult.MODEL_KEY_PREFIX + "testForm");

      // Whole model saved to flash for the redirect target to restore.
      assertThat(redirectAttributes.getFlashAttributes())
          .containsKey(SplibWebConstants.KEY_SAVED_MODEL);

      // No app-specific redirect override configured -> ReturnUrlBuilder.forAbnormalEnd().
      assertThat(mav.getViewName()).isEqualTo("redirect:/account/testFunc/page");
    }

    @SuppressWarnings("null")
    @Test
    void nestedItemContainerCv__qualifiedNestedFieldError() {
      when(loginStateUtil.getLoginState()).thenReturn("account");

      TestFormWithNested form = new TestFormWithNested();
      Model model = modelWithForm(form, controller);
      stubModel(model);

      // TestRecordWithNested (ItemContainer, 1 level under the form) holds a further
      // @Valid AnyNotNullBean nestedBean - exercising a ConstraintViolation whose propertyPath
      // ("nestedBean") is itself non-empty before the form-relative prefix is added.
      ViolationException ex =
          new ViolationException(new Violations().validate(new TestRecordWithNested()));
      handler.handleViolationException(ex, null, redirectAttributes);

      BindingResult br = (BindingResult) model
          .getAttribute(BindingResult.MODEL_KEY_PREFIX + "testFormWithNested");
      assertThat(br.getFieldErrorCount()).isEqualTo(1);
      assertThat(br.getFieldErrors().get(0).getField())
          .isEqualTo("testRecord.nestedBean.name");
    }

    @SuppressWarnings("null")
    @Test
    void multiLevelAlreadyQualifiedPath_redirectUrlOnAppExceptionSet__usesCustomRedirect() {
      controller.setRedirectUrlOnAppException(ReturnUrlBuilder.ofPath("/custom/abnormal"));

      TestFormWithValidNested form = new TestFormWithValidNested();
      Model model = modelWithForm(form, controller);
      stubModel(model);

      // propertyPath arrives already fully qualified from the form root ("rec.acc.mailAddress"),
      // two levels deep through a @Valid-cascaded record and nested bean.
      ViolationException ex = new ViolationException(new Violations().validate(form));
      ModelAndView mav = handler.handleViolationException(ex, null, redirectAttributes);

      BindingResult br = (BindingResult) model
          .getAttribute(BindingResult.MODEL_KEY_PREFIX + "testFormWithValidNested");
      assertThat(br.getFieldErrors().get(0).getField()).isEqualTo("rec.acc.mailAddress");

      // Controller-specified override is used verbatim instead of forAbnormalEnd().
      assertThat(mav.getViewName()).isEqualTo("redirect:/custom/abnormal");
    }

    @SuppressWarnings("null")
    @Test
    void businessViolationNoPath__globalOnly_noFieldErrorFlash() {
      when(loginStateUtil.getLoginState()).thenReturn("account");

      TestForm form = new TestForm();
      Model model = modelWithForm(form, controller);
      stubModel(model);

      ViolationException ex = violationOf(new BusinessViolation(MSG1));
      handler.handleViolationException(ex, null, redirectAttributes);

      BindingResult br = (BindingResult) model
          .getAttribute(BindingResult.MODEL_KEY_PREFIX + "testForm");
      assertThat(br.getFieldErrorCount()).isEqualTo(0);
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);

      // No field errors were produced, so nothing needs to be snapshotted to flash.
      assertThat(redirectAttributes.getFlashAttributes())
          .doesNotContainKey(SplibWebConstants.KEY_FLASH_FIELD_ERRORS);
    }
  }

  // =========================================================================
  // handleConstraintViolationException
  //
  // Wraps a plain jakarta.validation ConstraintViolationException into a ViolationException
  // and delegates to handleViolationException - verified here via the no-controller path
  // (simplest observable outcome: flashed global errors), since the delegation itself is
  // already covered above.
  // =========================================================================

  @Nested
  class HandleConstraintViolationException {

    private final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    @BeforeEach
    void setUpNoController() {
      when(request.getLocale()).thenReturn(Locale.ROOT);
    }

    @SuppressWarnings("null")
    private Set<ConstraintViolation<?>> cvsOf(Object bean) {
      return new HashSet<>(new Violations().validate(bean).getConstraintViolations());
    }

    @Test
    void plainCve__delegatesAndFlashesGlobalErrors() {
      ConstraintViolationException cve = new ConstraintViolationException(cvsOf(new CvBean()));

      handler.handleConstraintViolationException(cve, null, redirectAttributes);

      @SuppressWarnings("unchecked")
      List<String> errors = (List<String>) redirectAttributes.getFlashAttributes()
          .get(SplibWebConstants.KEY_GLOBAL_ERRORS);
      assertThat(errors).hasSize(1);
    }

    @SuppressWarnings("null")
    @Test
    void cveWithParameters__messageParametersPropagateToTheFinalMessage() {
      MessageParameters params =
          new MessageParameters(Boolean.FALSE, "PREFIX-", "-SUFFIX", false);
      ConstraintViolationExceptionWithParameters cve =
          new ConstraintViolationExceptionWithParameters(cvsOf(new CvBean()), params);

      handler.handleConstraintViolationException(cve, null, redirectAttributes);

      @SuppressWarnings("unchecked")
      List<String> errors = (List<String>) redirectAttributes.getFlashAttributes()
          .get(SplibWebConstants.KEY_GLOBAL_ERRORS);
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0)).startsWith("PREFIX-").endsWith("-SUFFIX");
    }
  }

  // =========================================================================
  // handleRedirectNeededExceptions
  // =========================================================================

  @Nested
  class HandleRedirectNeededExceptions {

    private final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    @Test
    void noResourceFoundException_modelAbsent__wrapsToHomePage_flashesMessage_savesNewModel() {
      // getModel() (from the request attribute) is left unstubbed -> null, matching the real
      // situation this exception fires in: before any controller's prepare() ran.
      when(request.getLocale()).thenReturn(Locale.ROOT);
      Model newModel = new ExtendedModelMap();
      NoResourceFoundException nrfe =
          new NoResourceFoundException(HttpMethod.GET, "No static resource foo/bar.", "foo/bar");

      ModelAndView mav =
          handler.handleRedirectNeededExceptions(nrfe, newModel, redirectAttributes);

      // Wrapped into RedirectToHomePageException -> redirects to the configured home page.
      assertThat(mav.getViewName()).isEqualTo("redirect:/top");

      // No form/BindingResult available (model was absent) -> message flashed as a global error.
      @SuppressWarnings("unchecked")
      List<String> errors = (List<String>) redirectAttributes.getFlashAttributes()
          .get(SplibWebConstants.KEY_GLOBAL_ERRORS);
      assertThat(errors).containsExactly("URL path not found. (URL path : foo/bar)");

      // The caller-supplied newModel (not getModel(), which was null) is the one saved to flash.
      assertThat(redirectAttributes.getFlashAttributes())
          .containsKey(SplibWebConstants.KEY_SAVED_MODEL);
    }

    @Test
    void redirectException_noMessageId__noMessageAdded_existingModelSaved() {
      Model model = new ExtendedModelMap();
      stubModel(model);

      RedirectException ex = new RedirectException("/some/path");
      ModelAndView mav = handler.handleRedirectNeededExceptions(ex, null, redirectAttributes);

      assertThat(mav.getViewName()).isEqualTo("redirect:/some/path");
      assertThat(redirectAttributes.getFlashAttributes())
          .doesNotContainKey(SplibWebConstants.KEY_GLOBAL_ERRORS);
      assertThat(redirectAttributes.getFlashAttributes())
          .containsKey(SplibWebConstants.KEY_SAVED_MODEL);
    }

    @SuppressWarnings("null")
    @Test
    void redirectException_withFormsInModel_andLogLevel__addsBusinessViolationToBindingResult() {
      when(request.getLocale()).thenReturn(Locale.ROOT);
      TestForm form = new TestForm();
      Model model = modelWithForm(form, new TestController("testFunc", new TestService()));
      stubModel(model);

      // Also exercises the logLevel branch (no assertion on the log output itself).
      RedirectException ex =
          new RedirectException("/some/path", Level.WARN, "log message", MSG1);
      handler.handleRedirectNeededExceptions(ex, null, redirectAttributes);

      BindingResult br =
          (BindingResult) model.getAttribute(BindingResult.MODEL_KEY_PREFIX + "testForm");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getGlobalErrors().get(0).getDefaultMessage()).isEqualTo("Test violation 1");

      // Routed to the form's BindingResult, not flashed as a raw string.
      assertThat(redirectAttributes.getFlashAttributes())
          .doesNotContainKey(SplibWebConstants.KEY_GLOBAL_ERRORS);
    }
  }

  // =========================================================================
  // handleOptimisticLockingFailureException
  // =========================================================================

  @Nested
  class HandleOptimisticLockingFailureException {

    private final RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    private static final String OPTIMISTIC_LOCKING_MSG =
        "Another user has updated the data. Reload the displayed data and check what's been "
            + "changed.";

    @BeforeEach
    void setUpLocale() {
      when(request.getLocale()).thenReturn(Locale.ROOT);
    }

    @Test
    void editController__redirectsToDefaultDestOnNormalEnd_withFlashedMessage() {
      TestEditController editController = new TestEditController("editFunc");
      Model model = new ExtendedModelMap();
      model.addAttribute(SplibWebConstants.KEY_CONTROLLER, editController);
      model.addAttribute("loginState", "account");
      stubModel(model);

      ModelAndView mav = handler.handleOptimisticLockingFailureException(
          new OverlappingFileLockException(), null, model, redirectAttributes);

      // pageTemplatePattern=SINGLE -> getDefaultDestSubFunctionOnNormalEnd()="edit";
      // getDefaultDestPageOnNormalEnd()="page" (not overridden).
      assertThat(mav.getViewName()).isEqualTo("redirect:/account/editFunc/edit/page");

      // No forms in this model -> message flashed as a global error (not a BindingResult).
      @SuppressWarnings("unchecked")
      List<String> errors = (List<String>) redirectAttributes.getFlashAttributes()
          .get(SplibWebConstants.KEY_GLOBAL_ERRORS);
      assertThat(errors).containsExactly(OPTIMISTIC_LOCKING_MSG);
    }

    @SuppressWarnings("null")
    @Test
    void nonEditController__delegatesToViolationExceptionHandling() {
      when(loginStateUtil.getLoginState()).thenReturn("account");

      TestController controller = new TestController("testFunc", new TestService());
      TestForm form = new TestForm();
      Model model = modelWithForm(form, controller);
      stubModel(model);

      ModelAndView mav = handler.handleOptimisticLockingFailureException(
          new OverlappingFileLockException(), null, model, redirectAttributes);

      // Not a SplibEditController -> wrapped as a BusinessViolation and routed through
      // handleViolationException/handleViolationExceptionWithController, so the message ends
      // up on the form's BindingResult rather than a raw flash string.
      BindingResult br =
          (BindingResult) model.getAttribute(BindingResult.MODEL_KEY_PREFIX + "testForm");
      assertThat(br.getGlobalErrorCount()).isEqualTo(1);
      assertThat(br.getGlobalErrors().get(0).getDefaultMessage())
          .isEqualTo(OPTIMISTIC_LOCKING_MSG);

      assertThat(mav.getViewName()).isEqualTo("redirect:/account/testFunc/page");
    }
  }

  // =========================================================================
  // handleThrowable
  // =========================================================================

  @Nested
  class HandleThrowable {

    @Test
    void actionOnThrowableNull_modelPresentInRequest__usesRequestModel_noActionInvoked() {
      Model requestModel = new ExtendedModelMap();
      requestModel.addAttribute("marker", "fromRequest");
      stubModel(requestModel);

      Model argModel = new ExtendedModelMap();
      argModel.addAttribute("marker", "fromArgument");

      ModelAndView mav = handler.handleThrowable(new RuntimeException("boom"), argModel);

      assertThat(mav.getViewName()).isEqualTo("error");
      assertThat(mav.getStatus()).isEqualTo(HttpStatusCode.valueOf(500));
      // getModel() (from the request) takes priority over the method's own model argument.
      assertThat(mav.getModel()).isEqualTo(requestModel.asMap());
    }

    @Test
    void actionOnThrowablePresent_modelAbsentFromRequest__actionInvoked_usesArgumentModel() {
      // request.getAttribute(KEY_MODEL) left unstubbed -> getModel() is null.
      @SuppressWarnings("null")
      SplibExceptionHandlerAction action = mock(SplibExceptionHandlerAction.class);
      SplibExceptionHandler handlerWithAction =
          new SplibExceptionHandler(request, action, loginStateUtil) {};

      Model argModel = new ExtendedModelMap();
      argModel.addAttribute("marker", "fromArgument");
      RuntimeException ex = new RuntimeException("boom");

      ModelAndView mav = handlerWithAction.handleThrowable(ex, argModel);

      verify(action, times(1)).execute(ex);
      assertThat(mav.getModel()).isEqualTo(argModel.asMap());
    }
  }
}
