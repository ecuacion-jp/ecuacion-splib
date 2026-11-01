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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.splib.core.audit.AuditLogWriter.Entry;
import jp.ecuacion.splib.core.audit.AuditLogWriter.TargetData;
import jp.ecuacion.splib.core.audit.enums.AuditLogEventType;
import jp.ecuacion.splib.core.audit.enums.AuditLogResult;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link AuditLogWriter}. */
@DisplayName("AuditLogWriter")
class AuditLogWriterTest {

  private static final OffsetDateTime TIMESTAMP =
      OffsetDateTime.of(2026, 10, 29, 12, 34, 56, 0, ZoneOffset.ofHours(9));

  @Test
  @DisplayName("toJson: every item is written in order, details at the top level, "
      + "with null as JSON null")
  void allItems() {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("systemCode", "S1");
    attributes.put("tenant", "T1");
    Map<String, @Nullable String> details = new LinkedHashMap<>();
    details.put("clientIp", "127.0.0.1");
    details.put("sessionIdSuffix", null);
    details.put("uri", "/account/sample/action");
    Entry record = new Entry(TIMESTAMP, "op-1",
        AuditLogEventType.OPERATION, AuditLogResult.SUCCESS, "user1", null, details,
        List.of(new TargetData("Sample", "1"), new TargetData("Other", "2")), null, attributes);

    assertThat(AuditLogWriter.toJson(record)).isEqualTo("{"
        + "\"timestamp\":\"2026-10-29T12:34:56+09:00\",\"operationId\":\"op-1\","
        + "\"eventType\":\"OPERATION\","
        + "\"result\":\"SUCCESS\",\"actor\":\"user1\",\"originalActor\":null,"
        + "\"clientIp\":\"127.0.0.1\",\"sessionIdSuffix\":null,"
        + "\"uri\":\"/account/sample/action\","
        + "\"targets\":[{\"type\":\"Sample\",\"id\":\"1\"},{\"type\":\"Other\",\"id\":\"2\"}],"
        + "\"error\":null,\"attributes\":{\"systemCode\":\"S1\",\"tenant\":\"T1\"}}");
  }

  @Test
  @DisplayName("toJson: no targets / no attributes are written as an empty array / object")
  void empty() {
    Entry record = new Entry(TIMESTAMP, null, AuditLogEventType.LOGOUT,
        AuditLogResult.SUCCESS, null, null, Map.of(), List.of(), null, Map.of());

    assertThat(AuditLogWriter.toJson(record))
        .contains("\"targets\":[]").endsWith("\"attributes\":{}}");
  }

  @Test
  @DisplayName("toJson: quotes, backslashes and control characters are escaped, "
      + "so a typed-in value cannot forge another line")
  void escaped() {
    Entry record = new Entry(TIMESTAMP, null, AuditLogEventType.LOGIN,
        AuditLogResult.FAILURE, "a\"b\\c\nd\re\tf\u0001g", null, Map.of(),
        List.of(), null, Map.of());

    String json = AuditLogWriter.toJson(record);

    assertThat(json).contains("\"actor\":\"a\\\"b\\\\c\\nd\\re\\tf\\u0001g\"")
        .doesNotContain("\n", "\r");
  }
}
