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
package jp.ecuacion.splib.web.util;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SplibThymeleafStringUtil}.
 */
class SplibThymeleafStringUtilTest {

  private final SplibThymeleafStringUtil util = new SplibThymeleafStringUtil();

  @Test
  void isSelected_collection_comparesElementsExactly() {
    assertThat(util.isSelected(List.of("10", "20"), "10")).isTrue();
    assertThat(util.isSelected(List.of("10", "20"), "1")).isFalse();
    assertThat(util.isSelected(List.of("10", "20"), "0")).isFalse();
    assertThat(util.isSelected(List.of("1"), "10")).isFalse();
    assertThat(util.isSelected(Set.of(10L), "10")).isTrue();
    assertThat(util.isSelected(Arrays.asList("1", null), "2")).isFalse();
    assertThat(util.isSelected(List.of(), "1")).isFalse();
  }

  @Test
  void isSelected_array_comparesElementsExactly() {
    assertThat(util.isSelected(new String[] {"10", "20"}, "20")).isTrue();
    assertThat(util.isSelected(new String[] {"10", "20"}, "1")).isFalse();
    assertThat(util.isSelected(new long[] {10L}, "10")).isTrue();
    assertThat(util.isSelected(new long[] {10L}, "1")).isFalse();
  }

  /** Spring binds multiple checked values to a {@code String} field as comma-separated values. */
  @Test
  void isSelected_string_comparesCommaSeparatedValuesExactly() {
    assertThat(util.isSelected("10", "10")).isTrue();
    assertThat(util.isSelected("10", "1")).isFalse();
    assertThat(util.isSelected("10,20", "20")).isTrue();
    assertThat(util.isSelected("10, 20", "20")).isTrue();
    assertThat(util.isSelected("10,20", "2")).isFalse();
    assertThat(util.isSelected("", "1")).isFalse();
  }

  @Test
  void selectListMethodName_withoutLists() {
    assertThat(util.selectListMethodName("acc")).isEqualTo("getAccList");
    assertThat(util.selectListMethodName("acc.name")).isEqualTo("getAcc_NameList");
  }

  /** Indexes of lists are ignored so that rows in a table share the same method. */
  @Test
  void selectListMethodName_withLists_ignoresIndexes() {
    assertThat(util.selectListMethodName("rows[3].lang")).isEqualTo("getRows_LangList");
    assertThat(util.selectListMethodName("rows[].lang")).isEqualTo("getRows_LangList");
    assertThat(util.selectListMethodName("acc.rows[0].lang")).isEqualTo("getAcc_Rows_LangList");
  }

  @Test
  void isSelected_null_returnsFalse() {
    assertThat(util.isSelected(null, "1")).isFalse();
    assertThat(util.isSelected(List.of("1"), null)).isFalse();
  }
}
