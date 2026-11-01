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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.jspecify.annotations.Nullable;

/**
 * Holds one revision of the data change history: a set of changes made in one transaction,
 * with when, who and by which operation.
 *
 * <p>Each changed row of an entity with {@code @Audited} is kept in the entity's history table
 *     ({@code <table>_aud}) with the {@code rev} of this revision. The table and column names
 *     follow Envers' default revision table, except for the columns added by splib.</p>
 *
 * <p>{@code revtstmp} is a date-time column, not Envers' default number of milliseconds, so that
 *     {@code ecuacion-tool-housekeep-db} can delete old revisions by it; the history tables are
 *     deleted with them as related tables joined by {@code rev}.</p>
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(DataChangeRevisionListener.class)
public class DataChangeRevision {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @RevisionNumber
  @Column(name = "rev")
  private @Nullable Long rev;

  /** The server's own time zone, like the audit log. */
  @RevisionTimestamp
  @Column(name = "revtstmp")
  private @Nullable LocalDateTime timestamp;

  /**
   * The ID of the operation that made the changes, the same as {@code operationId} of the
   * operation's audit log record. {@code null} outside an operation (e.g. in batch).
   */
  @Column(name = "operation_id", length = 36)
  private @Nullable String operationId;

  /** Who made the changes, the same as {@code actor} of the audit log. */
  @Column(name = "actor")
  private @Nullable String actor;

  /** During a switch user, the administrator, the same as {@code originalActor}. */
  @Column(name = "original_actor")
  private @Nullable String originalActor;

  public @Nullable Long getRev() {
    return rev;
  }

  public @Nullable LocalDateTime getTimestamp() {
    return timestamp;
  }

  public @Nullable String getOperationId() {
    return operationId;
  }

  void setOperationId(@Nullable String operationId) {
    this.operationId = operationId;
  }

  public @Nullable String getActor() {
    return actor;
  }

  void setActor(@Nullable String actor) {
    this.actor = actor;
  }

  public @Nullable String getOriginalActor() {
    return originalActor;
  }

  void setOriginalActor(@Nullable String originalActor) {
    this.originalActor = originalActor;
  }
}
