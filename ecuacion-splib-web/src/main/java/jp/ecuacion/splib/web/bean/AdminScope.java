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
package jp.ecuacion.splib.web.bean;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Holds the range of data a logged-in admin account can see.
 *
 * <p>It's either unrestricted or a set of values of the admin scope column.
 *     An empty set doesn't mean unrestricted:
 *     no records are visible in the tables narrowed by the admin scope.</p>
 *
 * @see jp.ecuacion.splib.web.service.SplibDataStoreDependentControllerAdviceService#getAdminScope
 */
public final class AdminScope {

  private static final AdminScope UNRESTRICTED = new AdminScope(null);

  private final @Nullable Set<Object> values;

  private AdminScope(@Nullable Set<Object> values) {
    this.values = values;
  }

  /**
   * Returns the scope which doesn't restrict the data.
   *
   * @return the scope
   */
  public static AdminScope unrestricted() {
    return UNRESTRICTED;
  }

  /**
   * Returns the scope restricted to the values.
   *
   * @param values the values of the admin scope column. It can be empty, which means nothing is
   *     visible, and cannot contain {@code null}.
   * @return the scope
   */
  public static AdminScope of(Collection<?> values) {
    return new AdminScope(Set.copyOf(values));
  }

  /**
   * Returns whether the data is restricted by this scope.
   *
   * @return {@code true} if the data is not restricted
   */
  public boolean isUnrestricted() {
    return values == null;
  }

  /**
   * Returns the values of the admin scope column.
   *
   * @return the values, which may be empty
   * @throws IllegalStateException if this scope is unrestricted
   */
  public Set<Object> getValues() {
    if (values == null) {
      throw new IllegalStateException("An unrestricted admin scope doesn't have values.");
    }

    return Objects.requireNonNull(values);
  }
}
