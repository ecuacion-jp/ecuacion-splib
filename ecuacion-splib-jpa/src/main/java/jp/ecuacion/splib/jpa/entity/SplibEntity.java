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

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Provides the customized jpa entity.
 */
public abstract class SplibEntity {

  /**
   * Returns an array of fields which construct a unique
   * constraint connected to the natural key.
   * 
   * @return set of unique constraint column list.
   */
  public Set<List<@NonNull String>> getSetOfUniqueConstraintFieldList() {
    Set<List<@NonNull String>> rtnSet = new HashSet<>();

    Table table = Objects.requireNonNull(this.getClass().getAnnotation(Table.class));
    UniqueConstraint[] ucs = table.uniqueConstraints();

    if (ucs == null) {
      return rtnSet;
    }

    for (UniqueConstraint uc : ucs) {
      rtnSet.add(Arrays.asList(uc.columnNames()));
    }

    return rtnSet;
  }

  /**
   * Returns if the entity has natural keys.
   * 
   * @return has natural keys.
   */
  public boolean hasNaturalKey() {
    return getSetOfUniqueConstraintFieldList().size() != 0;
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
   * @return has soft-delete field.
   */
  public abstract boolean hasSoftDeleteField();

  /**
   * Returns field name array.
   * 
   * @return field name array.
   */
  public abstract String[] getFieldNameArr();
}
