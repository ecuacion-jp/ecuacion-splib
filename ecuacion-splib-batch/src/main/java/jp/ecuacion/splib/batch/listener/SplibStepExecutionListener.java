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
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Is a listener for steps.
 */
@Component
public class SplibStepExecutionListener implements StepExecutionListener {

  @Override
  public void beforeStep(StepExecution stepExecution) {
    SplibBatchAdvice.setCurrentStep(stepExecution.getStepName());
  }
}
