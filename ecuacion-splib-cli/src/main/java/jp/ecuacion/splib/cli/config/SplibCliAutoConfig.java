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
package jp.ecuacion.splib.cli.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Registers ecuacion-splib-cli's own beans (currently just {@code SplibExceptionHandler}) via
 * Spring Boot auto-configuration, so they are picked up regardless of the consuming app's own
 * base package — unlike {@code ecuacion-splib-batch}, apps using ecuacion-splib-cli are not
 * expected to declare a {@code @ComponentScan} of their own.
 */
@AutoConfiguration
@ComponentScan("jp.ecuacion.splib.cli.exceptionhandler")
public class SplibCliAutoConfig {

}
