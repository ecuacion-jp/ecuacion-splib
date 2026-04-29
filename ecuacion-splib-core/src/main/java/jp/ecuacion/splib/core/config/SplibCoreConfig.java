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

import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
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
   * Provides a {@link MessageSource} backed by
   * {@link jp.ecuacion.lib.core.util.PropertiesFileUtil}.
   *
   * <p>Basenames in {@code spring.messages.basename} that follow the
   * {@code messages_xxx} naming convention are registered via
   * {@link PropertiesFileUtil#addResourceBundlePostfix(String)}.
   * Other naming conventions are not supported and cause startup failure.</p>
   *
   * <p>The following {@code spring.messages.*} settings are not supported
   * and cause startup failure if configured:</p>
   * <ul>
   *   <li>{@code spring.messages.cache-duration}</li>
   *   <li>{@code spring.messages.fallback-to-system-locale=true}</li>
   *   <li>{@code spring.messages.encoding} (non-UTF-8 value)</li>
   * </ul>
   *
   * @param env the Spring environment
   * @return the message source
   */
  @Bean
  MessageSource messageSource(Environment env) {
    validateUnsupportedSettings(env);
    registerExtraBasenames(env);
    return new PropertiesFileUtilMessageSource();
  }

  /**
   * Throws {@link RuntimeException} if unsupported {@code spring.messages.*} settings are set.
   *
   * @param env the Spring environment
   */
  private void validateUnsupportedSettings(Environment env) {
    if (env.getProperty("spring.messages.cache-duration") != null) {
      throw new RuntimeException(
          "'spring.messages.cache-duration' is not supported when using ecuacion's "
          + "PropertiesFileUtil-based MessageSource. Message files are loaded once at startup "
          + "and runtime hot-reloading is not available. In modern CI/CD and cloud-native "
          + "environments, changing bundled message files without redeployment is considered "
          + "an anti-pattern. For dynamic configuration, consider using Spring Cloud Config "
          + "or an external configuration service instead.");
    }

    if ("true".equalsIgnoreCase(env.getProperty("spring.messages.fallback-to-system-locale"))) {
      throw new RuntimeException(
          "'spring.messages.fallback-to-system-locale=true' is not supported when using "
          + "ecuacion's PropertiesFileUtil-based MessageSource. Falling back to the OS locale "
          + "causes environment-dependent behavior — for example, Japanese messages may be "
          + "displayed on a developer's macOS machine but English messages on a Linux production "
          + "server. PropertiesFileUtil intentionally excludes the OS locale from fallback "
          + "candidates to ensure consistent behavior across all environments. "
          + "Please set 'spring.messages.fallback-to-system-locale=false', which is also the "
          + "recommended setting in modern Spring Boot applications.");
    }

    String encoding = env.getProperty("spring.messages.encoding");
    if (encoding != null && !"UTF-8".equalsIgnoreCase(encoding)) {
      throw new RuntimeException(
          "'spring.messages.encoding' is not supported when using ecuacion's "
          + "PropertiesFileUtil-based MessageSource. Since Java 9, ResourceBundle defaults to "
          + "UTF-8, and this library requires JDK 21 or later, so UTF-8 is always used. "
          + "UTF-8 is the universal standard encoding capable of representing all languages "
          + "including Japanese, Chinese, and European characters. If you have legacy properties "
          + "files in a non-UTF-8 encoding, please convert them to UTF-8.");
    }
  }

  /**
   * Registers extra basenames from {@code spring.messages.basename} with
   * {@link PropertiesFileUtil}.
   *
   * <p>Base filenames without a postfix ({@code messages}, {@code item_names}, etc.) are
   * skipped since {@link PropertiesFileUtil} already covers them.
   * Basenames following the {@code basename_xxx} convention (e.g. {@code messages_xxx},
   * {@code item_names_xxx}) register {@code xxx} as a postfix.
   * Other naming conventions are not supported.</p>
   *
   * @param env the Spring environment
   */
  private void registerExtraBasenames(Environment env) {
    String basenames = env.getProperty("spring.messages.basename", "messages");
    for (String rawBasename : basenames.split(",")) {
      String basename = rawBasename.trim();
      // Extract the filename part from paths like "classpath:/i18n/messages_myapp".
      String filename = basename.substring(basename.lastIndexOf('/') + 1);

      if (filename.equals("messages") || filename.equals("item_names")
          || filename.equals("enum_names") || filename.equals("messages_with_item_names")) {
        // Base filenames without postfix; already covered by PropertiesFileUtil.
        continue;
      } else if (filename.startsWith("messages_with_item_names_")) {
        PropertiesFileUtil.addResourceBundlePostfix(
            filename.substring("messages_with_item_names_".length()));
      } else if (filename.startsWith("messages_")) {
        PropertiesFileUtil.addResourceBundlePostfix(filename.substring("messages_".length()));
      } else if (filename.startsWith("item_names_")) {
        PropertiesFileUtil.addResourceBundlePostfix(filename.substring("item_names_".length()));
      } else if (filename.startsWith("enum_names_")) {
        PropertiesFileUtil.addResourceBundlePostfix(filename.substring("enum_names_".length()));
      } else {
        throw new RuntimeException(
            "Non-standard basename '" + basename + "' in 'spring.messages.basename' is not "
            + "supported when using ecuacion's PropertiesFileUtil-based MessageSource. "
            + "Only 'messages', 'messages_xxx', 'item_names', 'item_names_xxx', "
            + "'enum_names', 'enum_names_xxx', 'messages_with_item_names',"
            + " or 'messages_with_item_names_xxx' naming conventions are supported"
            + " (e.g., 'messages_myapp', 'item_names_myapp').");
      }
    }
  }
}
