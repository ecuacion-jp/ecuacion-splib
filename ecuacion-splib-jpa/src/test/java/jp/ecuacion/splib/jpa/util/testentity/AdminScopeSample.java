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
package jp.ecuacion.splib.jpa.util.testentity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.descriptor.java.LongJavaType;

/** An entity with the admin scope filter, in the form code-generator generates. */
@Entity
@FilterDef(name = "adminScopeFilter",
    parameters = @ParamDef(name = "targetSystemId", type = LongJavaType.class),
    defaultCondition = "TARGET_SYSTEM_ID in (:targetSystemId)")
@Filter(name = "adminScopeFilter")
public class AdminScopeSample {

  @Id
  private Long id;

  private Long targetSystemId;

  /** Constructs a new instance. */
  public AdminScopeSample() {}

  /** Constructs a new instance. */
  public AdminScopeSample(Long id, Long targetSystemId) {
    this.id = id;
    this.targetSystemId = targetSystemId;
  }

  /** Returns the ID. */
  public Long getId() {
    return id;
  }

  /** Returns the target system ID. */
  public Long getTargetSystemId() {
    return targetSystemId;
  }
}
