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
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 * Provides utility methods which helps to use filters easily.
 * 
 */
public abstract class SplibJpaFilterUtil {

  @PersistenceContext
  EntityManager em;

  private boolean usesSoftDeleteFeature;
  private boolean usesGroupFeature;
  private String groupColumnName;
  private boolean hasCustomGroupColumn;
  private String customGroupName;
  private String customGroupColumnName;

  /**
   * Constructs a new instance.
   * 
   * @param usesSoftDeleteFeature usesSoftDeleteFeature
   * @param usesGroupFeature usesGroupFeature
   * @param groupColumnName groupColumnName
   * @param hasCustomGroupColumn hasCustomGroupColumn
   * @param customGroupName customGroupName
   * @param customGroupColumnName customGroupColumnName
   */
  public SplibJpaFilterUtil(boolean usesSoftDeleteFeature, boolean usesGroupFeature,
      String groupColumnName, boolean hasCustomGroupColumn, String customGroupName,
      String customGroupColumnName) {
    this.usesSoftDeleteFeature = usesSoftDeleteFeature;
    this.usesGroupFeature = usesGroupFeature;
    this.groupColumnName = groupColumnName;
    this.hasCustomGroupColumn = hasCustomGroupColumn;
    this.customGroupName = customGroupName;
    this.customGroupColumnName = customGroupColumnName;
  }

  /**
   * Enables group filter.
   * 
   * @param groupId groupId
   */
  public void enableGroupFilter(Object groupId) {
    if (usesGroupFeature) {
      Session session = em.unwrap(Session.class);
      Filter filter = session.enableFilter("groupFilter");
      filter.setParameter(groupColumnName, groupId);

      if (hasCustomGroupColumn) {
        Filter filterAccGroup = session.enableFilter(customGroupName);
        filterAccGroup.setParameter(customGroupColumnName, groupId);
      }
    }
  }

  /**
   * Disables group filter.
   */
  public void disableGroupFilter() {
    if (usesGroupFeature) {
      Session session = em.unwrap(Session.class);
      session.disableFilter("groupFilter");

      if (hasCustomGroupColumn) {
        session.disableFilter(customGroupName);
      }
    }
  }

  /**
   * Enables soft delete filter.
   */
  public void enableSoftDeleteFilter() {
    if (usesSoftDeleteFeature) {
      Session session = em.unwrap(Session.class);
      session.enableFilter("softDeleteFilter");
    }
  }

  /**
   * Disables soft delete filter.
   */
  public void disableSoftDeleteFilter() {
    if (usesSoftDeleteFeature) {
      Session session = em.unwrap(Session.class);
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
