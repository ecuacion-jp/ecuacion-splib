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
package jp.ecuacion.splib.core.container;

import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DatetimeFormatParameters")
class DatetimeFormatParametersTest {

  @Nested
  @DisplayName("setZoneOffsetWithJsMinutes()")
  class SetZoneOffsetWithJsMinutes {

    @ParameterizedTest(name = "jsMinutes={0} => ZoneOffset({1}:{2})")
    @CsvSource({
        "-540, 9, 0",
        "0, 0, 0",
        "300, -5, 0",
        "-330, 5, 30"
    })
    @DisplayName("Converts JS getTimezoneOffset() value to ZoneOffset")
    void convertsJsMinutesToZoneOffset(int jsMinutes, int expectedHours, int expectedMinutes) {
      DatetimeFormatParameters params = new DatetimeFormatParameters();
      params.setZoneOffsetWithJsMinutes(jsMinutes);
      assertThat(params.getZoneOffset())
          .isEqualTo(ZoneOffset.ofHoursMinutes(expectedHours, expectedMinutes));
    }
  }
}
