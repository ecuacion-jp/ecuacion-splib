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
package jp.ecuacion.splib.webmvc.util;

import static org.assertj.core.api.Assertions.assertThat;

import jp.ecuacion.splib.core.annotation.ReadOnlyOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;

@DisplayName("ReadOnlyOperationUtil")
class ReadOnlyOperationUtilTest {

  static class SampleController {

    public void plain() {}

    @ReadOnlyOperation
    public void readOnly() {}

    @ReadOnlyOperation(false)
    public void notReadOnly() {}
  }

  /** Inherits {@code readOnly()}'s annotation through the overridden method. */
  static class SubController extends SampleController {

    @Override
    public void readOnly() {}
  }

  @ReadOnlyOperation
  static class ReadOnlyController {

    public void plain() {}

    @ReadOnlyOperation(false)
    public void notReadOnly() {}
  }

  private static boolean isReadOnly(String httpMethod, Object controller, String methodName)
      throws NoSuchMethodException {
    return ReadOnlyOperationUtil.isReadOnly(new MockHttpServletRequest(httpMethod, "/"),
        new HandlerMethod(controller, controller.getClass().getMethod(methodName)));
  }

  @ParameterizedTest(name = "{0}: {1}")
  @CsvSource({"GET, true", "HEAD, true", "OPTIONS, true", "TRACE, true", "POST, false",
      "PUT, false", "PATCH, false", "DELETE, false"})
  @DisplayName("without the annotation: decided by the HTTP method")
  void byHttpMethod(String httpMethod, boolean expected) throws Exception {
    assertThat(isReadOnly(httpMethod, new SampleController(), "plain")).isEqualTo(expected);
  }

  @Test
  @DisplayName("POST with @ReadOnlyOperation: read-only")
  void postWithAnnotation() throws Exception {
    assertThat(isReadOnly("POST", new SampleController(), "readOnly")).isTrue();
  }

  @Test
  @DisplayName("GET with @ReadOnlyOperation(false): not read-only")
  void getWithAnnotationFalse() throws Exception {
    assertThat(isReadOnly("GET", new SampleController(), "notReadOnly")).isFalse();
  }

  @Test
  @DisplayName("an override of a @ReadOnlyOperation method: read-only")
  void inheritedFromOverriddenMethod() throws Exception {
    assertThat(isReadOnly("POST", new SubController(), "readOnly")).isTrue();
  }

  @Test
  @DisplayName("a controller with @ReadOnlyOperation: read-only, unless the method says otherwise")
  void classAnnotation() throws Exception {
    assertThat(isReadOnly("POST", new ReadOnlyController(), "plain")).isTrue();
    assertThat(isReadOnly("POST", new ReadOnlyController(), "notReadOnly")).isFalse();
  }
}
