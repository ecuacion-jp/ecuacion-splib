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
import jp.ecuacion.splib.core.audit.AuditLogWriter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the audit log when {@value #PROP_KEY_ENABLED} is {@code true}.
 *
 * <p>Disabled by default; when disabled, nothing in this class is registered, so applications
 *     that do not enable it behave exactly as before. When enabled, it registers:</p>
 *
 * <ul>
 *   <li>{@link AuditLogger}, writing with {@link AuditLogWriter}.</li>
 *   <li>{@link AuditLogInterceptor}, which records operations, and the
 *       {@link HandlerExceptionResolver} it needs
 *       ({@link AuditLogInterceptor#exceptionRecorder}, placed ahead of all other
 *       resolvers).</li>
 *   <li>{@link AuditLogSecurityEventListener}, which records login, logout and switch user.
 *       </li>
 * </ul>
 *
 * <p>Scanned by {@code SplibWebConfig} and {@code SplibRestConfig}, so that an application using
 *     both modules still gets one of each.</p>
 */
// Named explicitly, so that an application's own class named AuditLogConfig does not clash.
@Configuration(value = "splibAuditLogConfig", proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "jp.ecuacion.splib.core.audit-log", name = "enabled",
    havingValue = "true")
public class AuditLogConfig implements WebMvcConfigurer {

  /** {@code application.properties} key enabling the audit log. */
  public static final String PROP_KEY_ENABLED = "jp.ecuacion.splib.core.audit-log.enabled";

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
  AuditLogger splibAuditLogger() {
    return new AuditLogger(new AuditLogWriter());
  }

  @Bean
  AuditLogInterceptor splibAuditLogInterceptor(AuditLogger auditLogger) {
    return new AuditLogInterceptor(auditLogger);
  }

  @Bean
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
