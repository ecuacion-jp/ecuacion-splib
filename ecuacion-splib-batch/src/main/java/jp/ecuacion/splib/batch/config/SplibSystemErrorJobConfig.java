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

import java.util.Objects;
import jp.ecuacion.splib.batch.exceptionhandler.SplibBatchExceptionHandler;
import jp.ecuacion.splib.batch.listener.SplibJobExecutionListener;
import jp.ecuacion.splib.batch.listener.SplibStepExecutionListener;
import jp.ecuacion.splib.batch.tasklet.SystemErrorTasklet;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provides a built-in {@code ecuacionSystemErrorJob}, for testing the system error
 * behavior (exception handling, logging, and so on) without requiring an actual bug.
 *
 * <p>Run it with {@code --spring.batch.job.name=ecuacionSystemErrorJob}.</p>
 *
 * <p>This configuration is loaded only when {@code spring.batch.job.name} is explicitly
 * set to {@code ecuacionSystemErrorJob}, so it never adds a second {@code Job} bean
 * to apps that only define their own job.</p>
 */
@Configuration
@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = "ecuacionSystemErrorJob")
public class SplibSystemErrorJobConfig extends SplibAppParentBatchConfig {

  /**
   * Constructs a new instance.
   *
   * @param jobExecutionListener jobExecutionListener
   * @param stepExecutionListener stepExecutionListener
   * @param exceptionHandler exceptionHandler
   */
  public SplibSystemErrorJobConfig(SplibJobExecutionListener jobExecutionListener,
      SplibStepExecutionListener stepExecutionListener,
      SplibBatchExceptionHandler exceptionHandler) {
    super(jobExecutionListener, stepExecutionListener, exceptionHandler);
  }

  @Bean(name = "ecuacionSystemErrorJob")
  Job ecuacionSystemErrorJob(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return preparedJobBuilder("ecuacionSystemErrorJob", jobRepository)
        .start(ecuacionSystemErrorJobStep1(jobRepository, transactionManager)).build();
  }

  @Bean
  Step ecuacionSystemErrorJobStep1(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return Objects.requireNonNull(preparedStepBuilder("ecuacionSystemErrorJobStep1", jobRepository,
        transactionManager, new SystemErrorTasklet())).build();
  }
}
