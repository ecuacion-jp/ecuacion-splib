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
package jp.ecuacion.splib.rest.apikey;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.splib.core.audit.AuditLogWriter;
import jp.ecuacion.splib.core.audit.AuditLogWriter.Entry;
import jp.ecuacion.splib.core.audit.enums.AuditLogEventType;
import jp.ecuacion.splib.core.audit.enums.AuditLogResult;
import jp.ecuacion.splib.webmvc.audit.AuditLogger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tests that {@link SplibApiKeyAuthenticationFilter} and
 * {@link SplibBuiltinApiKeyAuthenticationFilter} record rejections in the audit log.
 */
@DisplayName("X-Api-Key authentication filters: audit log")
class SplibApiKeyAuthenticationFilterAuditLogTest {

  private static final String KEY = "s3cr3t-key";
  private static final String WRONG_KEY = "wr0ng-key";

  private final List<Entry> records = new ArrayList<>();

  private final AuditLogger auditLogger = new AuditLogger(new AuditLogWriter() {
    @Override
    public void write(Entry record) {
      records.add(record);
    }
  });

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private static MockHttpServletResponse doFilter(Filter filter, @Nullable String apiKey,
      @Nullable String apiKeyId) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/key/sample");
    request.setRemoteAddr("192.0.2.1");
    if (apiKey != null) {
      request.addHeader(SplibApiKeyHeaders.API_KEY, apiKey);
    }
    if (apiKeyId != null) {
      request.addHeader(SplibApiKeyHeaders.API_KEY_ID, apiKeyId);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  private Entry onlyRecord() {
    assertThat(records).hasSize(1);
    return records.get(0);
  }

  @Nested
  @DisplayName("SplibApiKeyAuthenticationFilter")
  class ApiKey {

    private SplibApiKeyAuthenticationFilter filter() {
      SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
          (apiKeyId, presented) -> List.of(
              new SplibApiKeyExpectedValue(KEY, SplibApiKeyComparisonMode.PLAIN)));
      filter.setAuditLogger(auditLogger);
      return filter;
    }

    @Test
    @DisplayName("wrong key: LOGIN / FAILURE with the key ID, without the presented key")
    void mismatch() throws Exception {
      assertThat(doFilter(filter(), WRONG_KEY, "client-1").getStatus()).isEqualTo(401);

      Entry record = onlyRecord();
      assertThat(record.eventType()).isEqualTo(AuditLogEventType.LOGIN);
      assertThat(record.result()).isEqualTo(AuditLogResult.FAILURE);
      assertThat(record.actor()).isEqualTo("client-1");
      assertThat(record.details().get("clientIp")).isEqualTo("192.0.2.1");
      assertThat(record.details().get("uri")).isEqualTo("/api/key/sample");
      assertThat(record.error()).isEqualTo("X-Api-Key mismatch");
      assertThat(record.toString()).doesNotContain(WRONG_KEY);
    }

    @Test
    @DisplayName("missing key: recorded")
    void missing() throws Exception {
      doFilter(filter(), null, null);

      assertThat(onlyRecord().error()).isEqualTo("missing X-Api-Key header");
      assertThat(onlyRecord().actor()).isNull();
    }

    @Test
    @DisplayName("invalid key ID: recorded without the key ID")
    void invalidKeyId() throws Exception {
      doFilter(filter(), KEY, "not valid!");

      assertThat(onlyRecord().error()).isEqualTo("invalid X-Api-Key-Id header");
      assertThat(onlyRecord().actor()).isNull();
    }

    @Test
    @DisplayName("locked out: recorded")
    void lockedOut() throws Exception {
      MockEnvironment env = new MockEnvironment();
      env.setProperty("jp.ecuacion.splib.rest.api-key.rate-limit.max-failures", "1");
      SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(
          (apiKeyId, presented) -> List.of(
              new SplibApiKeyExpectedValue(KEY, SplibApiKeyComparisonMode.PLAIN)), env);
      filter.setAuditLogger(auditLogger);

      doFilter(filter, WRONG_KEY, null);
      doFilter(filter, KEY, null);

      assertThat(records).extracting(Entry::error)
          .containsExactly("X-Api-Key mismatch", "locked out after too many X-Api-Key mismatches");
    }

    @Test
    @DisplayName("correct key: nothing recorded by the filter")
    void matched() throws Exception {
      assertThat(doFilter(filter(), KEY, "client-1").getStatus()).isEqualTo(200);

      assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("no audit logger (audit log disabled): rejected as before")
    void noAuditLogger() throws Exception {
      SplibApiKeyAuthenticationFilter filter = new SplibApiKeyAuthenticationFilter(null);

      assertThat(doFilter(filter, KEY, null).getStatus()).isEqualTo(401);
      assertThat(records).isEmpty();
    }
  }

  @Nested
  @DisplayName("SplibBuiltinApiKeyAuthenticationFilter")
  class BuiltinApiKey {

    @Test
    @DisplayName("wrong key: LOGIN / FAILURE with the key ID")
    void mismatch() throws Exception {
      SplibBuiltinApiKeyAuthenticationFilter filter = new SplibBuiltinApiKeyAuthenticationFilter();
      filter.setAuditLogger(auditLogger);

      assertThat(doFilter(filter, WRONG_KEY, "ops-1").getStatus()).isEqualTo(401);

      assertThat(onlyRecord().result()).isEqualTo(AuditLogResult.FAILURE);
      assertThat(onlyRecord().actor()).isEqualTo("ops-1");
      assertThat(onlyRecord().error()).isEqualTo("X-Api-Key mismatch");
    }
  }
}
