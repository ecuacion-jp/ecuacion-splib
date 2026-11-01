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
package jp.ecuacion.splib.web.util.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import jp.ecuacion.lib.core.annotation.ItemNameKeyClass;
import jp.ecuacion.lib.core.item.Item;
import jp.ecuacion.lib.core.item.ItemContainer;
import jp.ecuacion.splib.core.audit.AuditLogContext;
import jp.ecuacion.splib.core.audit.AuditLogWriter.TargetData;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link AuditLogTargetUtil}. */
@DisplayName("AuditLogTargetUtil")
class AuditLogTargetUtilTest {

  /** Like a generated base record: {@code ItemNameKeyClass} on it. */
  @ItemNameKeyClass("acc")
  private abstract static class AccBaseRecord extends SplibRecord implements ItemContainer {
    @Override
    public Item[] customizedItems() {
      return new Item[] {};
    }
  }

  /** Like an application's record, extending the base record. */
  private static class AccEditRecord extends AccBaseRecord {
  }

  private static class NoKindRecord extends SplibRecord implements ItemContainer {
    @Override
    public Item[] customizedItems() {
      return new Item[] {};
    }
  }

  private static class TestForm extends SplibGeneralForm {
    @SuppressWarnings("unused")
    @Nullable
    AccEditRecord rec;

    @SuppressWarnings("unused")
    @Nullable
    NoKindRecord other;
  }

  private static AccEditRecord accWithIds(String ids) {
    AccEditRecord rec = new AccEditRecord();
    rec.setIds(ids);
    return rec;
  }

  @AfterEach
  void tearDown() {
    AuditLogContext.end();
  }

  @Test
  @DisplayName("targetOf: the own primary key (the first segment of ids) "
      + "with the capitalized ItemNameKeyClass of a superclass")
  void target() {
    assertThat(AuditLogTargetUtil.targetOf(accWithIds("5,3,2")))
        .isEqualTo(new TargetData("Acc", "5"));
  }

  @Test
  @DisplayName("targetOf: a negative ID is kept as is")
  void negativeId() {
    assertThat(AuditLogTargetUtil.targetOf(accWithIds("-1,3")))
        .isEqualTo(new TargetData("Acc", "-1"));
  }

  @Test
  @DisplayName("targetOf: null without an own ID, even with related records' IDs")
  void noId() {
    assertThat(AuditLogTargetUtil.targetOf(accWithIds(""))).isNull();
    assertThat(AuditLogTargetUtil.targetOf(accWithIds(",3"))).isNull();
  }

  @Test
  @DisplayName("targetOf: null without ItemNameKeyClass")
  void noKind() {
    NoKindRecord rec = new NoKindRecord();
    rec.setIds("5");

    assertThat(AuditLogTargetUtil.targetOf(rec)).isNull();
  }

  @Test
  @DisplayName("addTargetsOf: adds the root records with an ID and a kind, skipping the others")
  void addTargetsOf() {
    TestForm form = new TestForm();
    form.rec = accWithIds("5");
    NoKindRecord other = new NoKindRecord();
    other.setIds("6");
    form.other = other;
    TestForm empty = new TestForm();
    AuditLogContext.begin(null, null);

    AuditLogTargetUtil.addTargetsOf(form, empty);

    assertThat(Objects.requireNonNull(AuditLogContext.current()).getTargets())
        .containsExactly(new TargetData("Acc", "5"));
  }

  @Test
  @DisplayName("addTargetsOf: does nothing when the operation is not recorded")
  void notRecorded() {
    TestForm form = new TestForm();
    form.rec = accWithIds("5");

    AuditLogTargetUtil.addTargetsOf(form);

    assertThat(AuditLogContext.current()).isNull();
  }
}
