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

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link PropertiesFileUtilMessageSource} does not let {@code useCodeAsDefaultMessage}
 * clobber Spring's own {@code ProblemDetail} message resolution (see the class's Javadoc on
 * {@code getDefaultMessage} for the full explanation of the bug this guards against).
 */
@DisplayName("PropertiesFileUtilMessageSource")
class PropertiesFileUtilMessageSourceTest {

  private final PropertiesFileUtilMessageSource messageSource =
      new PropertiesFileUtilMessageSource();

  @Test
  @DisplayName("an unresolved 'problemDetail.*' code resolves to null, "
      + "so Spring falls back to the exception's own reason/title")
  void problemDetailCodeReturnsNullWhenUnresolved() {
    String result = messageSource.getMessage(
        "problemDetail.org.springframework.web.server.ResponseStatusException", null, null,
        Locale.ROOT);

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("an unresolved code outside 'problemDetail.' still echoes back as the code itself "
      + "(useCodeAsDefaultMessage is otherwise unchanged)")
  void nonProblemDetailCodeStillUsesCodeAsDefaultMessage() {
    String code = "some.made.up.key.that.does.not.exist";

    String result = messageSource.getMessage(code, null, null, Locale.ROOT);

    assertThat(result).isEqualTo(code);
  }
}
