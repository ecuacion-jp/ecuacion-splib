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

import jp.ecuacion.splib.batch.converter.RunIdAddingJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link RunIdAddingJobParametersConverter} as the {@code JobParametersConverter}
 * picked up by Spring Boot's {@code JobLauncherApplicationRunner}, so every job launched
 * from the command line gets a unique {@code run.id} parameter without relying on
 * {@code RunIdIncrementer} (see {@link RunIdAddingJobParametersConverter} for why that
 * matters).
 */
@AutoConfiguration
public class SplibBatchJobParametersConverterAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(JobParametersConverter.class)
  JobParametersConverter jobParametersConverter() {
    return new RunIdAddingJobParametersConverter();
  }
}
