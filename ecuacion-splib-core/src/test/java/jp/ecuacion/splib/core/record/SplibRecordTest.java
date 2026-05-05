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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    @DisplayName("シンプルなプロパティを取得できる")
    void simpleProperty() {
      ConcreteRecord rec = new ConcreteRecord("hello");
      assertThat(rec.getValue("name")).isEqualTo("hello");
    }

    @Test
    @DisplayName("ドット区切りのネストされたパスで値を取得できる")
    void nestedProperty() {
      ConcreteRecord inner = new ConcreteRecord("inner");
      ConcreteRecord outer = new ConcreteRecord("outer", inner);
      assertThat(outer.getValue("nested.name")).isEqualTo("inner");
    }

    @Test
    @DisplayName("ネストされたオブジェクトがnullの場合はnullを返す")
    void nestedPropertyWhenRelationIsNull() {
      ConcreteRecord rec = new ConcreteRecord("outer");
      assertThat(rec.getValue("nested.name")).isNull();
    }
  }
}
