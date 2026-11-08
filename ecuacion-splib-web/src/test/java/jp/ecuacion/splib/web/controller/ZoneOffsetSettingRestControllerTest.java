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
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

/** Tests for {@link ZoneOffsetSettingRestController}. */
class ZoneOffsetSettingRestControllerTest {

  @Test
  void storesAValidIntegerOffsetInTheSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    ZoneOffsetSettingRestController controller = new ZoneOffsetSettingRestController(request);

    controller.zoneOffset("-540");

    assertThat(Objects.requireNonNull(request.getSession()).getAttribute("zoneOffset"))
        .isEqualTo("-540");
  }

  @Test
  void ignoresANonNumericOffsetWithoutTouchingTheSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    ZoneOffsetSettingRestController controller = new ZoneOffsetSettingRestController(request);

    controller.zoneOffset("not-a-number");

    assertThat(Objects.requireNonNull(request.getSession()).getAttribute("zoneOffset")).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"1080", "-1080"})
  void storesAnOffsetAtTheBoundaryOfTheSupportedRange(String offset) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    ZoneOffsetSettingRestController controller = new ZoneOffsetSettingRestController(request);

    controller.zoneOffset(offset);

    assertThat(Objects.requireNonNull(request.getSession()).getAttribute("zoneOffset"))
        .isEqualTo(offset);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1081", "-1081", "2000000000", "-2147483648"})
  void ignoresAnOutOfRangeOffsetWithoutTouchingTheSession(String offset) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    ZoneOffsetSettingRestController controller = new ZoneOffsetSettingRestController(request);

    controller.zoneOffset(offset);

    assertThat(Objects.requireNonNull(request.getSession()).getAttribute("zoneOffset")).isNull();
  }
}
