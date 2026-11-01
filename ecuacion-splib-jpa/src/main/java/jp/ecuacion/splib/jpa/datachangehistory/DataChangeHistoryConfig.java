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
package jp.ecuacion.splib.jpa.datachangehistory;

import jp.ecuacion.splib.core.audit.AuditLogConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * Adds {@link DataChangeRevision} to the entities scanned, alongside the application's own
 * {@code @EntityScan}, when the data change history is enabled.
 *
 * <p>{@code ecuacion-splib-jpa} depends on Hibernate Envers optionally: only the applications
 *     that use the data change history add {@code hibernate-envers}, and Envers is active
 *     whenever it is on the classpath (Hibernate's own behavior). Without it, nothing here is
 *     registered.</p>
 *
 * <p>Envers can be turned off with Hibernate's own
 *     {@code spring.jpa.properties.hibernate.integration.envers.enabled=false}. Then
 *     {@link DataChangeRevision} is not scanned either, so that no {@code revinfo} is made.</p>
 */
// Named explicitly, so that an application's own class of the same name does not clash.
@Configuration(value = "splibDataChangeHistoryConfig", proxyBeanMethods = false)
@ConditionalOnClass(name = AuditLogConstants.DATA_CHANGE_HISTORY_CLASS)
@ConditionalOnProperty(name = DataChangeHistoryConfig.PROP_KEY_ENVERS_ENABLED,
    havingValue = "true", matchIfMissing = true)
@EntityScan(basePackageClasses = DataChangeRevision.class)
public class DataChangeHistoryConfig {

  /** Hibernate's own key turning Envers on or off, given through {@code spring.jpa.properties}. */
  static final String PROP_KEY_ENVERS_ENABLED =
      "spring.jpa.properties.hibernate.integration.envers.enabled";
}
