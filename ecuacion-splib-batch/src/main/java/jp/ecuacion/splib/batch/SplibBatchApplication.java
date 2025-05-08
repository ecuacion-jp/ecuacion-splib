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
package jp.ecuacion.splib.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Provides default BatchApplication.
 * 
 * <p>We want it to be {@code abstract} to clarify 
 *    that this class is supposed to be extended to use.<br>
 *    But it's not allowed by spring. The following error message obtained.<br>
 *    {@code BeanCreationException: Error creating bean with name 'splibBatchApplication': 
 *    Failed to instantiate [jp.ecuacion.splib.batch.SplibBatchApplication]: 
 *    Is it an abstract class?}</p>
 */
public class SplibBatchApplication {

  /**
   * Has what needs to be done as the main method of the spring batch.
   * 
   * <p>java command doesn't seem to start 
   *     by calling the main method in the parent class of the class specified.<br>
   *     So you need to implement main mathod in the BatchApplication class in your each app
   *     and call {@code SplibBatchApplication.main(args)} in it.</p>
   */
  public static void main(Class<?> cls, String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(cls, args);
    System.exit(SpringApplication.exit(context, () -> 0));
  }
}
