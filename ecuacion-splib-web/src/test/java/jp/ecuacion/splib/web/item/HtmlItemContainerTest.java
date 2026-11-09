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
package jp.ecuacion.splib.web.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.splib.core.record.SplibRecord;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link HtmlItemContainer}. */
@DisplayName("HtmlItemContainer")
class HtmlItemContainerTest {

  /** A row of the list in {@link ParentRecord}. */
  public static class RowRecord extends SplibRecord {
    @SuppressWarnings("unused")
    @Nullable
    String name;

    @SuppressWarnings("unused")
    @Nullable
    String note;
  }

  /**
   * A record with a list of rows.
   *
   * <p>{@code @SuppressWarnings("null")} is needed because Eclipse null analysis reports
   *     {@code getNewItem()} of {@code HtmlItemContainer} and {@code ItemContainer}
   *     as incompatible when implemented outside a {@code @NullMarked} package.</p>
   */
  @SuppressWarnings("null")
  public static class ParentRecord extends SplibRecord implements HtmlItemContainer {
    @SuppressWarnings("unused")
    @Nullable
    String code;

    @SuppressWarnings("unused")
    List<RowRecord> rows = new ArrayList<>();

    @Override
    public HtmlItem[] customizedItems() {
      return new HtmlItem[] {new HtmlItem("code").isNotEmpty(true),
          new HtmlItem("rows[].name").isNotEmpty(true), new HtmlItem("rows[].note")};
    }
  }

  @Nested
  @DisplayName("isNotEmpty()")
  class IsNotEmpty {

    private final ParentRecord rec = new ParentRecord();

    @Test
    @DisplayName("a path without lists is judged as before")
    void pathWithoutLists() {
      assertThat(rec.isNotEmpty("code", "public", "")).isTrue();
    }

    @Test
    @DisplayName("an indexless path in a list is judged")
    void indexlessPath() {
      assertThat(rec.isNotEmpty("rows[].name", "public", "")).isTrue();
      assertThat(rec.isNotEmpty("rows[].note", "public", "")).isFalse();
    }

    @Test
    @DisplayName("a path with an index is judged in the same way as the indexless path")
    void pathWithIndex() {
      assertThat(rec.isNotEmpty("rows[3].name", "public", "")).isTrue();
      assertThat(rec.isNotEmpty("rows[3].note", "public", "")).isFalse();
    }
  }
}
