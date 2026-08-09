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
package jp.ecuacion.splib.batch.converter;

import java.util.Properties;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.job.parameters.JobParameters;

/**
 * Adds a unique {@code run.id} parameter to every job launch, taking over the role
 * {@code RunIdIncrementer} used to play.
 *
 * <p>Since Spring Batch 6, {@code JobOperator.start(Job, JobParameters)} discards
 * <strong>all</strong> parameters supplied at launch time (e.g. command-line arguments
 * converted by Spring Boot's {@code JobLauncherApplicationRunner}) whenever the
 * {@code Job} has a {@code JobParametersIncrementer} configured &mdash; see the
 * {@code JobOperator#start} Javadoc: "the incrementer will be used to calculate the next
 * parameters in the sequence and the provided parameters will be ignored." The
 * {@code JobParametersBuilder(JobParameters, JobExplorer)#getNextJobParameters(Job)}
 * helper that used to merge an incrementer's value with caller-supplied parameters
 * (Spring Batch 5 and earlier) was removed along with this change, so combining
 * {@code RunIdIncrementer} with launch-time parameters such as {@code excelPath} is no
 * longer possible via {@code Job#getJobParametersIncrementer()}.</p>
 *
 * <p>This converter sidesteps the problem at its source: jobs built via
 * {@code SplibAppParentBatchConfig#preparedJobBuilder} no longer set an incrementer at
 * all, so {@code JobOperator.start()} always takes its "no incrementer" branch and uses
 * the supplied parameters as-is. The uniqueness across launches that
 * {@code RunIdIncrementer} used to provide &mdash; so the same parameters can be run
 * again without a {@code JobInstanceAlreadyCompleteException} &mdash; is instead achieved
 * here, by stamping every launch with its own identifying {@code run.id} parameter before
 * the standard string-to-{@code JobParameters} conversion runs.</p>
 */
public class RunIdAddingJobParametersConverter extends DefaultJobParametersConverter {

  @Override
  public JobParameters getJobParameters(Properties properties) {
    Properties propertiesWithRunId = new Properties();
    propertiesWithRunId.putAll(properties);
    propertiesWithRunId.setProperty("run.id", String.valueOf(System.currentTimeMillis()));
    return super.getJobParameters(propertiesWithRunId);
  }
}
