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
package jp.ecuacion.splib.core.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;

/**
 * Provides configs for core.
 */
@Configuration
public class SplibCoreConfig {

  /**
   * Needs to use {@code @Value}.
   */
  @Bean
  PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  /**
   * Provides a {@link MessageSource} backed by {@link
   * jp.ecuacion.lib.core.util.PropertiesFileUtil}.
   *
   * <p>Basenames other than {@code messages} configured in
   * {@code spring.messages.basename} are handled by a parent
   * {@link ResourceBundleMessageSource}.</p>
   *
   * @param env the Spring environment
   * @return the message source
   */
  @Bean
  public MessageSource messageSource(Environment env) {
    PropertiesFileUtilMessageSource source = new PropertiesFileUtilMessageSource();

    String basenames = env.getProperty("spring.messages.basename", "messages");
    List<String> extras = Arrays.stream(basenames.split(","))
        .map(String::trim)
        .filter(b -> !b.equals("messages"))
        .toList();

    if (!extras.isEmpty()) {
      ResourceBundleMessageSource parent = new ResourceBundleMessageSource();
      parent.setBasenames(extras.toArray(new String[0]));
      source.setParentMessageSource(parent);
    }

    return source;
  }
}
