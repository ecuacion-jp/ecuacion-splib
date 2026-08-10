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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/** Tests for {@link SplibCoreConfig}. */
@DisplayName("SplibCoreConfig")
class SplibCoreConfigTest {

  private final SplibCoreConfig config = new SplibCoreConfig();

  private StandardEnvironment environmentWith(Map<String, Object> properties) {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
    return environment;
  }

  @Nested
  @DisplayName("messageSource: validateUnsupportedSettings")
  class ValidateUnsupportedSettings {

    @Test
    @DisplayName("spring.messages.always-use-message-format set (any value): rejected")
    void alwaysUseMessageFormatSetIsRejected() {
      var env = environmentWith(Map.of("spring.messages.always-use-message-format", "true"));

      assertThatThrownBy(() -> config.messageSource(env))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("spring.messages.always-use-message-format");
    }

    @Test
    @DisplayName("spring.messages.use-code-as-default-message=false: rejected "
        + "(conflicts with the hardcoded 'true' behavior)")
    void useCodeAsDefaultMessageFalseIsRejected() {
      var env = environmentWith(Map.of("spring.messages.use-code-as-default-message", "false"));

      assertThatThrownBy(() -> config.messageSource(env))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("spring.messages.use-code-as-default-message");
    }

    @Test
    @DisplayName("spring.messages.use-code-as-default-message=true: allowed "
        + "(matches existing hardcoded behavior)")
    void useCodeAsDefaultMessageTrueIsAllowed() {
      var env = environmentWith(Map.of("spring.messages.use-code-as-default-message", "true"));

      assertThat(config.messageSource(env)).isInstanceOf(PropertiesFileUtilMessageSource.class);
    }

    @Test
    @DisplayName("spring.messages.common-messages set: rejected")
    void commonMessagesSetIsRejected() {
      var env =
          environmentWith(Map.of("spring.messages.common-messages[0]", "classpath:common.properties"));

      assertThatThrownBy(() -> config.messageSource(env))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("spring.messages.common-messages");
    }

    @Test
    @DisplayName("none of the unsupported settings present: messageSource is created normally")
    void noUnsupportedSettingsIsAllowed() {
      var env = environmentWith(Map.of());

      assertThat(config.messageSource(env)).isInstanceOf(PropertiesFileUtilMessageSource.class);
    }
  }

  @Nested
  @DisplayName("messageSource: jp.ecuacion.splib.core.messages.use-spring-native toggle")
  class MessageSourceToggle {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class))
        .withUserConfiguration(SplibCoreConfig.class);

    @Test
    @DisplayName("unset: PropertiesFileUtilMessageSource is used (default)")
    void defaultsToPropertiesFileUtilMessageSource() {
      runner.run(context -> assertThat(context.getBean(MessageSource.class))
          .isInstanceOf(PropertiesFileUtilMessageSource.class));
    }

    @Test
    @DisplayName("=false: PropertiesFileUtilMessageSource is used")
    void explicitFalseUsesPropertiesFileUtilMessageSource() {
      runner.withPropertyValues("jp.ecuacion.splib.core.messages.use-spring-native=false")
          .run(context -> assertThat(context.getBean(MessageSource.class))
              .isInstanceOf(PropertiesFileUtilMessageSource.class));
    }

    @Test
    @DisplayName("=true: SplibCoreConfig's bean backs off, "
        + "Spring Boot's own ResourceBundleMessageSource is used instead")
    void trueUsesSpringNativeMessageSource() {
      // spring.messages.basename must point at a basename that actually exists on the classpath
      // (messages_splib_core.properties, this module's own bundled file) — otherwise Boot's
      // MessageSourceAutoConfiguration backs off too (its ResourceBundleCondition finds nothing
      // under the default "messages" basename), leaving no MessageSource bean at all.
      runner.withPropertyValues("jp.ecuacion.splib.core.messages.use-spring-native=true",
          "spring.messages.basename=messages_splib_core")
          .run(context -> assertThat(context.getBean(MessageSource.class))
              .isInstanceOf(ResourceBundleMessageSource.class)
              .isNotInstanceOf(PropertiesFileUtilMessageSource.class));
    }
  }
}
