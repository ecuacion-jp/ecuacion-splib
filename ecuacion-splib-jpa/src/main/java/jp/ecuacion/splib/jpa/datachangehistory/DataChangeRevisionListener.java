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

import java.util.Objects;
import jp.ecuacion.splib.core.audit.AuditLogContext;
import org.hibernate.envers.RevisionListener;
import org.jspecify.annotations.Nullable;

/**
 * Fills a new {@link DataChangeRevision} with the operation being processed on the current
 * thread: its ID, who does it and, during a switch user, the administrator.
 *
 * <p>They are kept by {@code ecuacion-splib-webmvc} during a request to a controller method
 *     ({@link AuditLogContext}), the same values as the operation's audit log record. Outside
 *     such a request (e.g. in batch, or in a filter such as the login), they are left
 *     {@code null}.</p>
 */
public class DataChangeRevisionListener implements RevisionListener {

  @Override
  public void newRevision(@Nullable Object revisionEntity) {
    AuditLogContext context = AuditLogContext.current();
    if (context == null) {
      return;
    }

    DataChangeRevision revision = Objects.requireNonNull((DataChangeRevision) revisionEntity);
    revision.setOperationId(context.getOperationId());
    revision.setActor(context.getActor());
    revision.setOriginalActor(context.getOriginalActor());
  }
}
