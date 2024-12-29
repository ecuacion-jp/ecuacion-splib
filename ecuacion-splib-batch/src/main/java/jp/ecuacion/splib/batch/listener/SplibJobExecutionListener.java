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
package jp.ecuacion.splib.batch.listener;

import jp.ecuacion.splib.batch.advice.SplibBatchAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Is a listener for jobs.
 */
@Component
public class SplibJobExecutionListener implements JobExecutionListener {

  protected static final Logger logger = LoggerFactory.getLogger("summary-logger");

  @Override
  public void beforeJob(JobExecution jobExecution) {
    logger.info("START: job-name: " + jobExecution.getJobInstance().getJobName());
    SplibBatchAdvice.setCurrentJob(jobExecution.getJobInstance().getJobName());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    if (jobExecution.getStatus().isUnsuccessful()) {
      logger.error("END  : job-name: " + jobExecution.getJobInstance().getJobName()
          + " [ABNORMAL END] exit status: " + jobExecution.getExitStatus().getExitCode());
      // jobExecution.getAllFailureExceptions().forEach(Throwable::printStackTrace);

    } else {
      // バッチが正常に終了した場合
      logger
          .info("END  : job-name: " + jobExecution.getJobInstance().getJobName() + " [NORMAL END]");
    }
  }
}
