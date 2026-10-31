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
package jp.ecuacion.splib.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import jp.ecuacion.splib.core.audit.AuditLogWriter.TargetData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link AuditLogContext}. */
@DisplayName("AuditLogContext")
class AuditLogContextTest {

  @AfterEach
  void tearDown() {
    AuditLogContext.end();
  }

  @Test
  @DisplayName("addTarget: the same data is added once, in the order first added")
  void addTargetOnce() {
    AuditLogContext.begin();

    AuditLogUtil.addTarget("Acc", 5L);
    AuditLogUtil.addTarget("Acc", "6");
    AuditLogUtil.addTarget("Acc", "5");

    assertThat(Objects.requireNonNull(AuditLogContext.current()).getTargets())
        .containsExactly(new TargetData("Acc", "5"), new TargetData("Acc", "6"));
  }
}
