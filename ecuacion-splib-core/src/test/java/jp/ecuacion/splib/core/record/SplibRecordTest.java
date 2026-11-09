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
package jp.ecuacion.splib.core.record;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SplibRecord")
class SplibRecordTest {

  static class ConcreteRecord extends SplibRecord {

    private String name;
    private @Nullable ConcreteRecord nested;

    ConcreteRecord(String name) {
      this.name = name;
    }

    ConcreteRecord(String name, ConcreteRecord nested) {
      this.name = name;
      this.nested = nested;
    }

    public String getName() {
      return name;
    }

    public @Nullable ConcreteRecord getNested() {
      return nested;
    }
  }

  @Nested
  @DisplayName("getValue()")
  class GetValue {

    @Test
    @DisplayName("Returns a simple property value")
    void simpleProperty() {
      ConcreteRecord rec = new ConcreteRecord("hello");
      assertThat(rec.getValue("name")).isEqualTo("hello");
    }

    @Test
    @DisplayName("Returns a value via dot-separated nested path")
    void nestedProperty() {
      ConcreteRecord inner = new ConcreteRecord("inner");
      ConcreteRecord outer = new ConcreteRecord("outer", inner);
      assertThat(outer.getValue("nested.name")).isEqualTo("inner");
    }

    @Test
    @DisplayName("Returns null when a nested object is null")
    void nestedPropertyWhenRelationIsNull() {
      ConcreteRecord rec = new ConcreteRecord("outer");
      assertThat(rec.getValue("nested.name")).isNull();
    }
  }

  // Classes for getStringLength().
  // The registered lengths are static and shared by all the tests,
  // so each test uses its own classes or field names.

  static class LenRecordA extends SplibRecord {
    static {
      getStringLengthMap(LenRecordA.class).put("name", 100);
      getStringLengthMap(LenRecordA.class).put("code", null);
    }
  }

  static class LenRecordB extends SplibRecord {
    static {
      getStringLengthMap(LenRecordB.class).put("name", 50);
    }
  }

  static class LenRecordASub extends LenRecordA {
    static {
      getStringLengthMap(LenRecordASub.class).put("code", 10);
    }
  }

  /** Not instantiated in the tests, so its static initializer runs only when it's resolved. */
  static class LenNestedRecord extends SplibRecord {
    static {
      getStringLengthMap(LenNestedRecord.class).put("name", 30);
    }
  }

  /** A row class which isn't a SplibRecord. */
  static class LenRow {
    public @Nullable String getName() {
      return null;
    }
  }

  static class LenParentRecord extends SplibRecord {
    static {
      getStringLengthMap(LenParentRecord.class).put("name", 70);
      getStringLengthMap(LenRow.class).put("name", 20);
    }

    public @Nullable LenNestedRecord getNested() {
      return null;
    }

    public List<LenRow> getRows() {
      return new ArrayList<>();
    }

    public LenRow[] getRowArray() {
      return new LenRow[0];
    }
  }

  @SuppressWarnings("deprecation")
  static class LegacyRecord extends SplibRecord {
    static {
      getStringLengthMap().put("legacyOnlyField", 40);
    }

    public @Nullable LenRow getRow() {
      return null;
    }
  }

  @Nested
  @DisplayName("getStringLength()")
  class GetStringLength {

    @Test
    @DisplayName("Fields with the same name in different records return their own lengths")
    void sameFieldNameInDifferentRecords() {
      assertThat(new LenRecordA().getStringLength("name")).isEqualTo(100);
      assertThat(new LenRecordB().getStringLength("name")).isEqualTo(50);
    }

    @Test
    @DisplayName("A subclass inherits the lengths of its superclass and can override them")
    void subclass() {
      LenRecordASub rec = new LenRecordASub();
      assertThat(rec.getStringLength("name")).isEqualTo(100);
      assertThat(rec.getStringLength("code")).isEqualTo(10);
    }

    @Test
    @DisplayName("Returns null when the field is registered with null")
    void registeredWithNull() {
      assertThat(new LenRecordA().getStringLength("code")).isNull();
    }

    @Test
    @DisplayName("Resolves the class of a nested record, running its static initializer")
    void nestedRecord() {
      LenParentRecord rec = new LenParentRecord();
      assertThat(rec.getStringLength("name")).isEqualTo(70);
      assertThat(rec.getStringLength("nested.name")).isEqualTo(30);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rows[3].name", "rows[].name", "rowArray[0].name"})
    @DisplayName("Resolves the element class of a list or an array from an indexed path")
    void indexedPath(String itemPropertyPath) {
      assertThat(new LenParentRecord().getStringLength(itemPropertyPath)).isEqualTo(20);
    }

    @Test
    @DisplayName("Falls back to the deprecated map when not registered for the class")
    void fallbackToDeprecatedMap() {
      LegacyRecord rec = new LegacyRecord();
      assertThat(rec.getStringLength("legacyOnlyField")).isEqualTo(40);
      assertThat(rec.getStringLength("row.legacyOnlyField")).isEqualTo(40);
    }

    @Test
    @DisplayName("Falls back to the deprecated map by the last field name "
        + "when the path can't be resolved")
    void fallbackWhenPathNotResolved() {
      assertThat(new LegacyRecord().getStringLength("noSuchField.legacyOnlyField"))
          .isEqualTo(40);
    }

    @Test
    @DisplayName("Returns null when not registered anywhere")
    void notRegistered() {
      assertThat(new LenRecordB().getStringLength("notRegisteredField")).isNull();
    }
  }
}
