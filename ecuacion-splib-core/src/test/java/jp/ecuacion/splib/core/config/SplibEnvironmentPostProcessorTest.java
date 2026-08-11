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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Verifies the resolver {@link SplibEnvironmentPostProcessor#postProcessEnvironment} registers:
 * once it has run, {@link PropertiesFileUtil}'s own {@code application.properties} lookups
 * (used by, e.g., {@link jp.ecuacion.lib.core.util.LocaleUtil}) delegate entirely to the Spring
 * {@code Environment} — including a value that exists only there, such as one Spring Boot
 * itself merged in from an externalized {@code application.properties} location (e.g.
 * {@code file:./config/application.properties} next to an executable jar/war) that is not on
 * the classpath.
 */
@DisplayName("SplibEnvironmentPostProcessor")
class SplibEnvironmentPostProcessorTest {

  private final SplibEnvironmentPostProcessor processor = new SplibEnvironmentPostProcessor();

  @AfterEach
  void clearResolver() {
    PropertiesFileUtil.setApplicationResolver(null);
  }

  @Test
  @DisplayName("postProcessEnvironment: a key present only in the Environment "
      + "(simulating an externally placed application.properties) "
      + "becomes visible via PropertiesFileUtil.getApplication")
  void postProcessEnvironment_exposesEnvironmentOnlyKeyToPropertiesFileUtil() {
    String key = "jp.ecuacion.locale.use-root";

    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources()
        .addFirst(new MapPropertySource("simulatedExternalConfig", Map.of(key, "true")));

    // Not visible before the resolver is registered: PropertiesFileUtil only knows the
    // classpath, and splib-core bundles no application.properties of its own.
    assertThat(PropertiesFileUtil.hasApplication(key)).isFalse();

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(PropertiesFileUtil.hasApplication(key)).isTrue();
    assertThat(PropertiesFileUtil.getApplication(key)).isEqualTo("true");
  }
}
