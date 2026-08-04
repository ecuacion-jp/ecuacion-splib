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
package jp.ecuacion.splib.batch.config;

import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provides {@code @ComponentScan}s.
 */
@Configuration
@ComponentScan("jp.ecuacion.splib.core.config"
    + ",jp.ecuacion.splib.batch.advice"
    + ",jp.ecuacion.splib.batch.listener"
    + ",jp.ecuacion.splib.batch.exceptionhandler"
    )
public class SplibBatchConfig {

  /**
   * Provides a no-op {@code PlatformTransactionManager} for the Spring Batch step machinery
   * so a real database connection (e.g. {@code spring-boot-starter-jdbc}) is not required.
   *
   * <p>Backs off via {@code @ConditionalOnMissingBean} when an app declares
   * {@code spring-boot-starter-jdbc} and configures a real datasource: Boot's
   * {@code DataSourceTransactionManagerAutoConfiguration} then supplies the real
   * transaction manager instead, which is needed if the tasklet itself performs
   * Spring-managed JDBC/JPA transactions against a real database.</p>
   */
  @Bean
  @ConditionalOnMissingBean(PlatformTransactionManager.class)
  PlatformTransactionManager transactionManager() {
    return new ResourcelessTransactionManager();
  }
}
