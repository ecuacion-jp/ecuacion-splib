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
package jp.ecuacion.splib.jpa.datachangehistory.testentity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.envers.Audited;
import org.jspecify.annotations.Nullable;

/** An entity with {@code @Audited}, like the ones {@code ecuacion-tool-code-generator} makes. */
@Entity
@Audited
public class HistorySample {

  @Id
  private @Nullable Long id;

  private @Nullable String name;

  /** Constructs an instance, for JPA. */
  public HistorySample() {}

  /** Constructs an instance with the values. */
  public HistorySample(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public @Nullable Long getId() {
    return id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public @Nullable String getName() {
    return name;
  }
}
