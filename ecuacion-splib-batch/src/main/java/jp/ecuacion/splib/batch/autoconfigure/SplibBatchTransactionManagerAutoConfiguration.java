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
package jp.ecuacion.splib.batch.autoconfigure;

import javax.sql.DataSource;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Supplies a no-op {@code PlatformTransactionManager} for the Spring Batch step machinery
 * when no {@code DataSource} bean exists, so a real database connection (e.g.
 * {@code spring-boot-starter-jdbc}) is not required to run a batch job.
 *
 * <p>Registered as a Spring Boot auto-configuration (not a plain
 * {@code @Configuration}/{@code @ComponentScan} bean) so it is sorted with, and runs after,
 * {@code DataSourceAutoConfiguration} in the deferred auto-configuration phase. That ordering
 * matters: a {@code @ConditionalOnMissingBean} bean defined in a component-scanned
 * {@code @Configuration} class is registered in the eager phase, before auto-configurations
 * are even evaluated, so it would win by default and shadow a real, DataSource-backed
 * transaction manager (e.g. {@code JpaTransactionManager}) even when one is available.</p>
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
public class SplibBatchTransactionManagerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(DataSource.class)
  PlatformTransactionManager transactionManager() {
    return new ResourcelessTransactionManager();
  }
}
