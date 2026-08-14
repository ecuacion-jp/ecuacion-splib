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
package jp.ecuacion.splib.web.form;

import static org.assertj.core.api.Assertions.assertThat;
import jp.ecuacion.lib.core.item.Item;
import jp.ecuacion.lib.core.item.ItemContainer;
import jp.ecuacion.splib.core.record.SplibRecord;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SplibGeneralForm#toItemPropertyPath}.
 */
class SplibGeneralFormTest {

  private static class TestRecord extends SplibRecord implements ItemContainer {
    @SuppressWarnings("unused")
    @Nullable
    String name;

    @Override
    public Item[] customizedItems() {
      return new Item[] {};
    }
  }

  private static class TestForm extends SplibGeneralForm {
    @SuppressWarnings("unused")
    TestRecord testRecord = new TestRecord();
  }

  @Test
  void pathWithRootRecordFieldPrefix_prefixIsStripped() {
    assertThat(new TestForm().toItemPropertyPath("testRecord.name")).isEqualTo("name");
  }

  @Test
  void nestedPathWithRootRecordFieldPrefix_onlyRootPrefixIsStripped() {
    assertThat(new TestForm().toItemPropertyPath("testRecord.acc.mailAddress"))
        .isEqualTo("acc.mailAddress");
  }

  @Test
  void pathWithoutAnyRootRecordFieldPrefix_isReturnedUnchanged() {
    assertThat(new TestForm().toItemPropertyPath("name")).isEqualTo("name");
  }

  @Test
  void pathWithUnrelatedFieldNamePrefix_isNotStripped() {
    // "testRecordExtra.name" must not be mistaken for a "testRecord."-prefixed path.
    assertThat(new TestForm().toItemPropertyPath("testRecordExtra.name"))
        .isEqualTo("testRecordExtra.name");
  }
}
