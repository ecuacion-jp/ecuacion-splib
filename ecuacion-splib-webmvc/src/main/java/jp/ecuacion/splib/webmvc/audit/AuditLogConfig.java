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

import java.util.List;
import jp.ecuacion.splib.core.audit.AuditLogConstants;
import jp.ecuacion.splib.core.audit.AuditLogWriter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the audit log when {@code jp.ecuacion.splib.core.audit-log.enabled}
 * ({@link AuditLogConstants#PROP_KEY_ENABLED}) is {@code true}, and what the data change history
 * needs when Hibernate Envers is on the classpath
 * ({@link AuditLogConstants#DATA_CHANGE_HISTORY_CLASS}).
 *
 * <p>When neither applies, nothing in this class is registered, so applications that use
 *     neither behave exactly as before.</p>
 *
 * <ul>
 *   <li>{@link AuditLogInterceptor}, and the {@link HandlerExceptionResolver} it needs
 *       ({@link AuditLogInterceptor#exceptionRecorder}, placed ahead of all other resolvers):
 *       registered when either is enabled. It keeps who does the operation and its ID during
 *       the request ({@code AuditLogContext}), which the data change history reads, and records
 *       operations when the audit log is enabled.</li>
 *   <li>{@link AuditLogger}, writing with {@link AuditLogWriter}, and
 *       {@link AuditLogSecurityEventListener}, which records login, logout and switch user:
 *       registered only when the audit log is enabled.</li>
 * </ul>
 *
 * <p>Scanned by {@code SplibWebConfig} and {@code SplibRestConfig}, so that an application using
 *     both modules still gets one of each.</p>
 */
// Named explicitly, so that an application's own class named AuditLogConfig does not clash.
@Configuration(value = "splibAuditLogConfig", proxyBeanMethods = false)
@Conditional(AuditLogConfig.AuditLogOrDataChangeHistory.class)
public class AuditLogConfig implements WebMvcConfigurer {

  /** Matches when the audit log is enabled or the data change history is used. */
  static class AuditLogOrDataChangeHistory extends AnyNestedCondition {

    AuditLogOrDataChangeHistory() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(name = AuditLogConstants.PROP_KEY_ENABLED, havingValue = "true")
    static class AuditLog {
    }

    @ConditionalOnClass(name = AuditLogConstants.DATA_CHANGE_HISTORY_CLASS)
    static class DataChangeHistory {
    }
  }

  private final ObjectProvider<AuditLogInterceptor> interceptor;

  /**
   * Constructs a new instance.
   *
   * @param interceptor the interceptor registered in {@link #addInterceptors}
   */
  public AuditLogConfig(ObjectProvider<AuditLogInterceptor> interceptor) {
    this.interceptor = interceptor;
  }

  @Bean
  @ConditionalOnProperty(name = AuditLogConstants.PROP_KEY_ENABLED, havingValue = "true")
  AuditLogger splibAuditLogger() {
    return new AuditLogger(new AuditLogWriter());
  }

  @Bean
  AuditLogInterceptor splibAuditLogInterceptor(ObjectProvider<AuditLogger> auditLogger) {
    return new AuditLogInterceptor(auditLogger.getIfAvailable());
  }

  @Bean
  @ConditionalOnProperty(name = AuditLogConstants.PROP_KEY_ENABLED, havingValue = "true")
  AuditLogSecurityEventListener splibAuditLogSecurityEventListener(
      AuditLogger auditLogger) {
    return new AuditLogSecurityEventListener(auditLogger);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(interceptor.getObject());
  }

  @Override
  public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
    resolvers.add(0, AuditLogInterceptor.exceptionRecorder());
  }
}
