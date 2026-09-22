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
package jp.ecuacion.splib.jpa.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SplibEntity")
class SplibEntityTest {

  /** Concrete entity with no natural key and no indexes at all. */
  @Table(name = "no_key_entity")
  static class NoKeyEntity extends SplibEntity {
    @Override
    public void preInsert() {}

    @Override
    public void preUpdate() {}

    @Override
    public boolean hasSoftDeleteField() {
      return false;
    }
  }

  /** Concrete entity whose natural key is expressed via {@code @UniqueConstraint} only. */
  @Table(name = "natural_key_entity",
      uniqueConstraints = {@UniqueConstraint(columnNames = {"mailAddress"})})
  static class NaturalKeyEntity extends SplibEntity {
    @Override
    public void preInsert() {}

    @Override
    public void preUpdate() {}

    @Override
    public boolean hasSoftDeleteField() {
      return false;
    }
  }

  /**
   * Concrete entity with a unique index but no {@code @UniqueConstraint} (no natural key) - the
   * scenario {@link SplibEntity#hasNaturalKey()} must not be fooled by.
   */
  @Table(name = "unique_index_only_entity",
      indexes = {@Index(name = "idx_code", columnList = "code", unique = true)})
  static class UniqueIndexOnlyEntity extends SplibEntity {
    @Override
    public void preInsert() {}

    @Override
    public void preUpdate() {}

    @Override
    public boolean hasSoftDeleteField() {
      return false;
    }
  }

  /**
   * Concrete entity with both a natural key ({@code @UniqueConstraint}) and an unrelated
   * multi-column unique index, plus a non-unique index that must be excluded from the unique
   * constraint set.
   */
  @Table(name = "mixed_entity",
      uniqueConstraints = {@UniqueConstraint(columnNames = {"mailAddress"})},
      indexes = {
          @Index(name = "idx_code_kind", columnList = "code, kind", unique = true),
          @Index(name = "idx_name", columnList = "name", unique = false)})
  static class MixedEntity extends SplibEntity {
    @Override
    public void preInsert() {}

    @Override
    public void preUpdate() {}

    @Override
    public boolean hasSoftDeleteField() {
      return false;
    }
  }

  @Nested
  @DisplayName("hasNaturalKey() / getNaturalKeyFieldList()")
  class NaturalKey {

    @Test
    @DisplayName("returns false / null when the entity has neither a unique constraint nor an "
        + "index")
    void noKey() {
      NoKeyEntity entity = new NoKeyEntity();
      assertThat(entity.hasNaturalKey()).isFalse();
      assertThat(entity.getNaturalKeyFieldList()).isNull();
    }

    @Test
    @DisplayName("returns true / the columns when the entity has a @UniqueConstraint")
    void withUniqueConstraint() {
      NaturalKeyEntity entity = new NaturalKeyEntity();
      assertThat(entity.hasNaturalKey()).isTrue();
      assertThat(entity.getNaturalKeyFieldList()).containsExactly("mailAddress");
    }

    @Test
    @DisplayName("returns false / null when the entity has only a unique @Index and no "
        + "@UniqueConstraint (a unique index is not a natural key)")
    void withUniqueIndexOnly() {
      UniqueIndexOnlyEntity entity = new UniqueIndexOnlyEntity();
      assertThat(entity.hasNaturalKey()).isFalse();
      assertThat(entity.getNaturalKeyFieldList()).isNull();
    }
  }

  @Nested
  @DisplayName("getSetOfUniqueConstraintFieldList()")
  class UniqueConstraintFieldList {

    @Test
    @DisplayName("returns an empty set when the entity has neither a unique constraint nor an "
        + "index")
    void noKey() {
      NoKeyEntity entity = new NoKeyEntity();
      assertThat(entity.getSetOfUniqueConstraintFieldList()).isEmpty();
    }

    @Test
    @DisplayName("includes the @UniqueConstraint columns")
    void withUniqueConstraint() {
      NaturalKeyEntity entity = new NaturalKeyEntity();
      assertThat(entity.getSetOfUniqueConstraintFieldList())
          .containsExactly(List.of("mailAddress"));
    }

    @Test
    @DisplayName("includes a unique @Index's columns even though it has no @UniqueConstraint")
    void withUniqueIndexOnly() {
      UniqueIndexOnlyEntity entity = new UniqueIndexOnlyEntity();
      assertThat(entity.getSetOfUniqueConstraintFieldList()).containsExactly(List.of("code"));
    }

    @Test
    @DisplayName("includes both the @UniqueConstraint and a unique multi-column @Index, but "
        + "excludes a non-unique @Index")
    void withMixedConstraintsAndIndexes() {
      MixedEntity entity = new MixedEntity();
      assertThat(entity.getSetOfUniqueConstraintFieldList()).containsExactlyInAnyOrder(
          List.of("mailAddress"), List.of("code", "kind"));
    }
  }
}
