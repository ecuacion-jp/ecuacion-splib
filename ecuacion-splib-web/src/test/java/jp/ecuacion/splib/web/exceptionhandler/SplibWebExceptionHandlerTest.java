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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jp.ecuacion.lib.core.exception.ConstraintViolationExceptionWithParameters;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.exception.ViolationWarningException;
import jp.ecuacion.lib.core.item.Item;
import jp.ecuacion.lib.core.item.ItemContainer;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.util.internal.PropertiesFileUtilBundleReader;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.core.violation.Violations.MessageParameters;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.web.bean.ReturnUrlBuilder;
import jp.ecuacion.splib.web.bean.WarnMessageBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibEditController;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.RedirectException;
import jp.ecuacion.splib.web.exception.ViolationWebWarningException;
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
 * Unit tests for {@link SplibWebExceptionHandler}'s {@code @ExceptionHandler} methods
 * ({@code handleWarning}, {@code handleViolationException}, and so on).
 *
 * <p>These tests verify the wiring around each handler - Model / RedirectAttributes /
 * SplibGeneralController interaction, redirect target selection, form preparation - rather
 * than the detailed violation-to-BindingResult path resolution, which is covered independently
 * by {@link jp.ecuacion.splib.web.exceptionhandler.internal.ViolationBindingResultMapperTest}.</p>
 *
 * <p>The test locale is fixed to {@code Locale.ROOT} so that
 * {@code messages_splib-web-test.properties} (no locale suffix) is resolved.</p>
 */
@ExtendWith(MockitoExtension.class)
class SplibWebExceptionHandlerTest {

  // message IDs defined in messages_splib-web-test.properties
  private static final String MSG1 = "jp.ecuacion.splib.web.test.violation1";
  private static final String MSG2 = "jp.ecuacion.splib.web.test.violation2";

  @BeforeAll
  static void init() {
    PropertiesFileUtilBundleReader.addToDynamicPostfixList("splib-web-test");
  }

  // application.properties values consumed by the handler methods under test (as opposed to
  // addViolationErrorsToBindingResult, which receives needsMsgAtItem / needsMsgAtTop as explicit arguments
  // and never reads these). Registered once via PropertiesFileUtil#setApplicationResolver
  // because RedirectToHomePageException reads "home-page" in a static initializer, so it must
  // be available before that class is first loaded by any test.
  private static final Map<String, String> APPLICATION_PROPS = Map.of(
      "jp.ecuacion.splib.web.home-page", "/top",
      SplibWebExceptionHandler.PROP_KEY_SHOWN_AT_EACH_ITEM, "true",
      SplibWebExceptionHandler.PROP_KEY_SHOWN_AT_THE_TOP, "true");

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
  private SplibWebExceptionHandler handler;

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

  // =========================================================================
  // Support classes for handler-level tests (handleWarning, handleViolationException, etc.)
  //
  // Unlike addViolationErrorsToBindingResult, these methods go through Model / RedirectAttributes /
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
    handler = new SplibWebExceptionHandler(request, null, loginStateUtil) {};
  }


  private ViolationException violationOf(BusinessViolation... bvs) {
    Violations violations = new Violations();
    for (BusinessViolation bv : bvs) {
      violations.add(bv);
    }
    return new ViolationException(violations);
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

      ModelAndView mav = handler.handleViolationWarningException(ex, null);

      WarnMessageBean bean =
          (WarnMessageBean) model.getAttribute(SplibWebConstants.KEY_WARN_MESSAGE);
      assertThat(bean).isNotNull();
      assertThat(Objects.requireNonNull(bean).getMessageId()).isEqualTo(MSG1);
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

      handler.handleViolationWarningException(ex, null);

      WarnMessageBean bean =
          (WarnMessageBean) model.getAttribute(SplibWebConstants.KEY_WARN_MESSAGE);
      assertThat(bean).isNotNull();
      assertThat(Objects.requireNonNull(bean).getMessage()).isEqualTo("Test violation 2");
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

      // addViolationErrorsToBindingResult delegation: field error resolved against the form's record.
      BindingResult br = (BindingResult) model
          .getAttribute(BindingResult.MODEL_KEY_PREFIX + "testForm");
      assertThat(Objects.requireNonNull(br).getFieldErrorCount()).isEqualTo(1);
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
    void redirectUrlOnAppExceptionSet__usesCustomRedirect() {
      // The detailed CV/BV-to-BindingResult path resolution is covered by
      // ViolationBindingResultMapperTest; this only verifies that a controller-specified
      // redirect override is honored instead of ReturnUrlBuilder.forAbnormalEnd().
      controller.setRedirectUrlOnAppException(ReturnUrlBuilder.ofPath("/custom/abnormal"));

      TestForm form = new TestForm();
      Model model = modelWithForm(form, controller);
      stubModel(model);

      ViolationException ex = new ViolationException(new Violations().validate(new CvBean()));
      ModelAndView mav = handler.handleViolationException(ex, null, redirectAttributes);

      BindingResult br = (BindingResult) model
          .getAttribute(BindingResult.MODEL_KEY_PREFIX + "testForm");
      assertThat(Objects.requireNonNull(br).getFieldErrors().get(0).getField())
          .isEqualTo("testRecord.name");

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
      assertThat(Objects.requireNonNull(br).getFieldErrorCount()).isEqualTo(0);
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
      assertThat(Objects.requireNonNull(errors).get(0)).startsWith("PREFIX-").endsWith("-SUFFIX");
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
      assertThat(Objects.requireNonNull(br).getGlobalErrorCount()).isEqualTo(1);
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
      assertThat(Objects.requireNonNull(br).getGlobalErrorCount()).isEqualTo(1);
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
      SplibWebExceptionHandler handlerWithAction =
          new SplibWebExceptionHandler(request, action, loginStateUtil) {};

      Model argModel = new ExtendedModelMap();
      argModel.addAttribute("marker", "fromArgument");
      RuntimeException ex = new RuntimeException("boom");

      ModelAndView mav = handlerWithAction.handleThrowable(ex, argModel);

      verify(action, times(1)).execute(ex);
      assertThat(mav.getModel()).isEqualTo(argModel.asMap());
    }
  }
}
