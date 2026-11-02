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
package jp.ecuacion.splib.jpa.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.Objects;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;

/**
 * Provides utility methods which helps to use filters easily.
 * 
 */
public abstract class SplibJpaFilterUtil {

  @PersistenceContext
  @Nullable
  EntityManager em;

  private boolean usesSoftDeleteFeature;
  private boolean usesGroupFeature;
  private @Nullable String groupColumnName;
  private Map<String, String> customGroupFilterParamNameMap;

  /**
   * Constructs a new instance.
   *
   * <p>A custom group filter is the filter of a table which has a group identifier column
   *     (a column treated as the group column under a different name, like the primary key of the
   *     table representing the group itself).
   *     Every custom group filter is enabled and disabled together with the common group filter,
   *     with the same group ID.</p>
   *
   * @param usesSoftDeleteFeature usesSoftDeleteFeature
   * @param usesGroupFeature usesGroupFeature
   * @param groupColumnName the parameter name of the common group filter,
   *     or {@code null} if the group feature is not used
   * @param customGroupFilterParamNameMap a map of custom group filter names
   *     to their parameter names; empty if no custom group filter exists
   */
  public SplibJpaFilterUtil(boolean usesSoftDeleteFeature, boolean usesGroupFeature,
      @Nullable String groupColumnName, Map<String, String> customGroupFilterParamNameMap) {
    this.usesSoftDeleteFeature = usesSoftDeleteFeature;
    this.usesGroupFeature = usesGroupFeature;
    this.groupColumnName = groupColumnName;
    this.customGroupFilterParamNameMap = Map.copyOf(customGroupFilterParamNameMap);
  }

  /**
   * Enables group filter.
   * 
   * @param groupId groupId
   */
  public void enableGroupFilter(Object groupId) {
    if (usesGroupFeature) {
      Session session = Objects.requireNonNull(Objects.requireNonNull(em).unwrap(Session.class));
      Filter filter = session.enableFilter("groupFilter");
      filter.setParameter(Objects.requireNonNull(groupColumnName), groupId);

      for (Map.Entry<String, String> entry : customGroupFilterParamNameMap.entrySet()) {
        session.enableFilter(entry.getKey()).setParameter(entry.getValue(), groupId);
      }
    }
  }

  /**
   * Disables group filter.
   */
  public void disableGroupFilter() {
    if (usesGroupFeature) {
      Session session = Objects.requireNonNull(Objects.requireNonNull(em).unwrap(Session.class));
      session.disableFilter("groupFilter");

      for (String customGroupFilterName : customGroupFilterParamNameMap.keySet()) {
        session.disableFilter(customGroupFilterName);
      }
    }
  }

  /**
   * Enables soft delete filter.
   */
  public void enableSoftDeleteFilter() {
    if (usesSoftDeleteFeature) {
      Session session = Objects.requireNonNull(Objects.requireNonNull(em).unwrap(Session.class));
      session.enableFilter("softDeleteFilter");
    }
  }

  /**
   * Disables soft delete filter.
   */
  public void disableSoftDeleteFilter() {
    if (usesSoftDeleteFeature) {
      Session session = Objects.requireNonNull(Objects.requireNonNull(em).unwrap(Session.class));
      session.disableFilter("softDeleteFilter");
    }
  }

  /**
   * Enables all filters.
   * 
   * @param groupId groupId
   */
  public void enableAllFilters(Object groupId) {
    enableSoftDeleteFilter();

    if (groupId != null) {
      enableGroupFilter(groupId);
    }
  }

  /**
   * Disables all filters.
   */
  public void disableAllFilters() {
    disableSoftDeleteFilter();

    disableGroupFilter();
  }
}
