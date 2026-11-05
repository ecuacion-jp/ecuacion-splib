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
package jp.ecuacion.splib.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import jp.ecuacion.splib.web.advice.SplibDataStoreDependentControllerAdvice;
import jp.ecuacion.splib.web.form.SplibEditForm;
import jp.ecuacion.splib.web.form.SplibListForm;
import jp.ecuacion.splib.web.form.SplibSearchForm;
import jp.ecuacion.splib.web.service.SplibDataStoreDependentControllerAdviceService;
import jp.ecuacion.splib.web.service.SplibEditService;
import jp.ecuacion.splib.web.service.SplibGeneralService;
import jp.ecuacion.splib.web.service.SplibSearchListService;
import jp.ecuacion.splib.web.util.SplibLoginStateUtil;
import jp.ecuacion.splib.web.util.internal.SplibControllerPrepareHelper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies that dependencies reach every level of the class hierarchy.
 *
 * <p>Several base classes declare a field of the same name. Their setters must not override each
 * other, otherwise Spring skips the overridden one and the field in the parent class stays
 * {@code null}.</p>
 */
@SuppressWarnings("null")
@DisplayName("Setter injection in the base classes apps extend")
class SplibBaseClassInjectionTest {

  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final SplibLoginStateUtil loginStateUtil = mock(SplibLoginStateUtil.class);
  private final SplibControllerPrepareHelper prepareHelper =
      mock(SplibControllerPrepareHelper.class);
  private final SplibDataStoreDependentControllerAdviceService adviceService =
      mock(SplibDataStoreDependentControllerAdviceService.class);
  private final TestSearchForm searchForm = new TestSearchForm();

  private AnnotationConfigApplicationContext context;

  @BeforeEach
  void setUp() {
    context = new AnnotationConfigApplicationContext();
    context.registerBean(HttpServletRequest.class, () -> request);
    context.registerBean(HttpServletResponse.class, () -> response);
    context.registerBean(SplibLoginStateUtil.class, () -> loginStateUtil);
    context.registerBean(SplibControllerPrepareHelper.class, () -> prepareHelper);
    context.registerBean(SplibDataStoreDependentControllerAdviceService.class, () -> adviceService);
    context.registerBean(TestSearchForm.class, () -> searchForm);
    context.registerBean(TestEditService.class, () -> mock(TestEditService.class));
    context.registerBean(TestSearchListService.class, () -> mock(TestSearchListService.class));
    context.registerBean(TestEditController.class);
    context.registerBean(TestSearchListController.class);
    context.registerBean(TestGeneralService.class);
    context.registerBean(TestAdvice.class);
    context.refresh();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Test
  @DisplayName("SplibEditController and its parents receive their dependencies")
  void editController() {
    TestEditController controller = context.getBean(TestEditController.class);

    assertThat(controller.request).isSameAs(request);
    assertThat(controller.response).isSameAs(response);
    assertThat(controller.serviceList).containsExactly(context.getBean(TestEditService.class));
    assertThat(field(controller, SplibGeneralController.class, "loginStateUtil"))
        .isSameAs(loginStateUtil);
    assertThat(field(controller, SplibGeneralController.class, "prepareHelper"))
        .isSameAs(prepareHelper);
    assertThat(field(controller, SplibEditController.class, "loginStateUtil"))
        .isSameAs(loginStateUtil);
  }

  @Test
  @DisplayName("SplibSearchListController and its parents receive their dependencies")
  void searchListController() {
    TestSearchListController controller = context.getBean(TestSearchListController.class);

    assertThat(controller.serviceList)
        .isEqualTo(List.of(context.getBean(TestSearchListService.class)));
    assertThat(field(controller, SplibGeneralController.class, "loginStateUtil"))
        .isSameAs(loginStateUtil);
    assertThat(field(controller, SplibSearchListController.class, "loginStateUtil"))
        .isSameAs(loginStateUtil);
    assertThat(field(controller, SplibSearchListController.class, "newSearchForm"))
        .isSameAs(searchForm);
  }

  @Test
  @DisplayName("SplibGeneralService receives the request")
  void generalService() {
    assertThat(field(context.getBean(TestGeneralService.class), SplibGeneralService.class,
        "request")).isSameAs(request);
  }

  @Test
  @DisplayName("SplibDataStoreDependentControllerAdvice receives its service")
  void advice() {
    assertThat(field(context.getBean(TestAdvice.class),
        SplibDataStoreDependentControllerAdvice.class, "service")).isSameAs(adviceService);
  }

  private static @Nullable Object field(Object target, Class<?> declaringClass, String name) {
    return ReflectionTestUtils.getField(target, declaringClass, name);
  }

  static class TestSearchForm extends SplibSearchForm {
    @Override
    protected String getDefaultSortItem() {
      return "";
    }
  }

  static class TestEditForm extends SplibEditForm {
  }

  abstract static class TestEditService extends SplibEditService<TestEditForm> {
  }

  abstract static class TestSearchListService
      extends SplibSearchListService<TestSearchForm, SplibListForm<?>> {
  }

  static class TestGeneralService extends SplibGeneralService {
  }

  static class TestAdvice extends SplibDataStoreDependentControllerAdvice {
  }

  static class TestEditController extends SplibEditController<TestEditForm, TestEditService> {
    TestEditController() {
      super(PageTemplatePatternEnum.SINGLE, "test");
    }
  }

  static class TestSearchListController
      extends SplibSearchListController<TestSearchForm, SplibListForm<?>, TestSearchListService> {
    TestSearchListController() {
      super("test");
    }
  }
}
