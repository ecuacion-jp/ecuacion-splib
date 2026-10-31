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
package jp.ecuacion.splib.webmvc.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/** Tests for {@link AuditLogConfig}. */
@DisplayName("AuditLogConfig")
class AuditLogConfigTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner().withUserConfiguration(AuditLogConfig.class);

  @Test
  @DisplayName("property not set: nothing is registered")
  void notSet() {
    runner.run(context -> {
      assertThat(context).doesNotHaveBean(AuditLogConfig.class);
      assertThat(context).doesNotHaveBean(AuditLogger.class);
      assertThat(context).doesNotHaveBean(AuditLogInterceptor.class);
      assertThat(context).doesNotHaveBean(AuditLogSecurityEventListener.class);
    });
  }

  @Test
  @DisplayName("enabled=false: nothing is registered")
  void disabled() {
    runner.withPropertyValues(AuditLogConfig.PROP_KEY_ENABLED + "=false")
        .run(context -> assertThat(context).doesNotHaveBean(AuditLogger.class));
  }

  @Test
  @DisplayName("enabled=true: the logger, the interceptor and the listener are registered")
  void enabled() {
    runner.withPropertyValues(AuditLogConfig.PROP_KEY_ENABLED + "=true").run(context -> {
      assertThat(context).hasSingleBean(AuditLogger.class);
      assertThat(context).hasSingleBean(AuditLogInterceptor.class);
      assertThat(context).hasSingleBean(AuditLogSecurityEventListener.class);
    });
  }
}
