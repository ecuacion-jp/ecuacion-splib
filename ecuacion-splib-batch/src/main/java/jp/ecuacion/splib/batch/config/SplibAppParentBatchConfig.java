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

import jp.ecuacion.splib.batch.exceptionhandler.SplibExceptionHandler;
import jp.ecuacion.splib.batch.listener.SplibJobExecutionListener;
import jp.ecuacion.splib.batch.listener.SplibStepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provides {@code ecuacion-splib} standard {@code JobBuilder} and {@code stepBuilder}.
 */
public abstract class SplibAppParentBatchConfig {

  @Autowired
  private SplibJobExecutionListener jobExecutionListener;
  @Autowired
  private SplibStepExecutionListener stepExecutionListener;
  @Autowired
  private SplibExceptionHandler exceptionHandler;

  /**
   * Provides {@code ecuacion-spilb} standard {@code JobBuilder}.
   * 
   * @param jobName jobName
   * @param jobRepository jobRepository
   * @return JobBuilder
   */
  protected JobBuilder preparedJobBuilder(String jobName, JobRepository jobRepository) {
    return new JobBuilder(jobName, jobRepository).incrementer(new RunIdIncrementer())
        .listener(jobExecutionListener);
  }

  /**
   * Provides {@code ecuacion-spilb} standard {@code StepBuilder}.
   * 
   * @param stepName stepName
   * @param jobRepository jobRepository
   * @param transactionManager transactionManager
   * @param tasklets tasklets
   * @return TaskletStepBuilder
   */
  protected TaskletStepBuilder preparedStepBuilder(String stepName, JobRepository jobRepository,
      PlatformTransactionManager transactionManager, Tasklet... tasklets) {
    StepBuilder builder = new StepBuilder(stepName, jobRepository).listener(stepExecutionListener);
    TaskletStepBuilder tlBuilder = null;
    for (Tasklet tasklet : tasklets) {
      if (tlBuilder == null) {
        tlBuilder = buildTasklet(builder, tasklet, transactionManager);

      } else {
        tlBuilder = buildTasklet(tlBuilder, tasklet, transactionManager);
      }
    }

    return tlBuilder;
  }

  private TaskletStepBuilder buildTasklet(Object builder, Tasklet tasklet,
      PlatformTransactionManager transactionManager) {
    TaskletStepBuilder tlBuilder = builder instanceof StepBuilder
        ? ((StepBuilder) builder).tasklet(tasklet, transactionManager)
        : ((TaskletStepBuilder) builder).tasklet(tasklet, transactionManager);
    
    return tlBuilder.exceptionHandler(exceptionHandler);
  }
}
