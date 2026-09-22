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

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Filter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Provides the customized jpa entity.
 */
public abstract class SplibEntity {

  private static final String SOFT_DELETE_FILTER_NAME = "softDeleteFilter";

  /**
   * Returns an array of fields which construct a unique
   * constraint connected to the natural key.
   *
   * <p>Reads every {@code @UniqueConstraint} declared in this entity's {@code @Table}
   *     annotation, plus every {@code @Index(unique = true)}. Note that this therefore also
   *     includes unique indexes unrelated to any natural key; use {@link
   *     #getNaturalKeyFieldList()} when the natural key specifically is needed.</p>
   *
   * @return set of unique constraint column list.
   */
  public Set<List<@NonNull String>> getSetOfUniqueConstraintFieldList() {
    Set<List<@NonNull String>> rtnSet = new HashSet<>();

    Table table = Objects.requireNonNull(this.getClass().getAnnotation(Table.class));
    UniqueConstraint[] ucs = table.uniqueConstraints();

    if (ucs != null) {
      for (UniqueConstraint uc : ucs) {
        rtnSet.add(Arrays.asList(uc.columnNames()));
      }
    }

    Index[] indexes = table.indexes();
    if (indexes != null) {
      for (Index index : indexes) {
        if (index.unique()) {
          rtnSet.add(Arrays.stream(index.columnList().split(",")).map(String::trim).toList());
        }
      }
    }

    return rtnSet;
  }

  /**
   * Returns the natural key field list, or {@code null} if this entity has none.
   *
   * <p>Reads only this entity's {@code @Table(uniqueConstraints = ...)} (at most one, by
   *     convention: the natural key). Deliberately does not go through {@link
   *     #getSetOfUniqueConstraintFieldList()}: once that also reports {@code @Index(unique =
   *     true)} columns unrelated to any natural key, picking an arbitrary entry from its result
   *     would no longer reliably identify the natural key.</p>
   *
   * @return natural key field list, or {@code null} if none.
   */
  public @Nullable List<String> getNaturalKeyFieldList() {
    Table table = Objects.requireNonNull(this.getClass().getAnnotation(Table.class));
    UniqueConstraint[] ucs = table.uniqueConstraints();

    if (ucs == null || ucs.length == 0) {
      return null;
    }

    return Arrays.asList(ucs[0].columnNames());
  }

  /**
   * Returns if the entity has natural keys.
   *
   * @return has natural keys.
   */
  public boolean hasNaturalKey() {
    return getNaturalKeyFieldList() != null;
  }

  /**
   * Provides preInsert procedure.
   * 
   * <p>When you use spring framework, this won't be used.</p>
   */
  public abstract void preInsert();

  /**
   * Provides preUpdate procedure.
   * 
   * <p>When you use spring framework, this won't be used.</p>
   */
  public abstract void preUpdate();

  /**
   * Returns if the entity has soft-delete field.
   *
   * <p>Determined dynamically: {@code true} if a {@code @Filter(name = "softDeleteFilter")}
   *     annotation is present on this entity's class or any of its superclasses (e.g. {@code
   *     SystemCommon}, when the soft-delete column is common to every entity rather than defined
   *     per-table).</p>
   *
   * @return has soft-delete field.
   */
  public boolean hasSoftDeleteField() {
    for (Class<?> clazz = this.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
      for (Filter filter : clazz.getAnnotationsByType(Filter.class)) {
        if (filter.name().equals(SOFT_DELETE_FILTER_NAME)) {
          return true;
        }
      }
    }

    return false;
  }
}
