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
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link PropertiesFileUtilMessageSource} does not let {@code useCodeAsDefaultMessage}
 * clobber Spring's own {@code ProblemDetail} message resolution (see the class's Javadoc on
 * {@code getDefaultMessage} for the full explanation of the bug this guards against), and that
 * argument-less messages read the same whether called directly via {@link PropertiesFileUtil}
 * or through this Spring-facing {@code MessageSource}.
 */
@DisplayName("PropertiesFileUtilMessageSource")
class PropertiesFileUtilMessageSourceTest {

  private final PropertiesFileUtilMessageSource messageSource =
      new PropertiesFileUtilMessageSource();

  @BeforeAll
  static void beforeAll() {
    PropertiesFileUtil.addResourceBundlePostfix("splib-core-test");
  }

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

  @Test
  @DisplayName("an argument-less message with a literal single quote resolves the same way "
      + "whether called directly via PropertiesFileUtil.getMessage or through this MessageSource")
  void argumentLessMessageWithQuoteMatchesDirectPropertiesFileUtilCall() {
    String code = "jp.ecuacion.splib.core.config.PropertiesFileUtilMessageSourceTest.quote";

    String direct = PropertiesFileUtil.getMessage(Locale.ROOT, code);
    String viaMessageSource = messageSource.getMessage(code, null, null, Locale.ROOT);

    assertThat(direct).isEqualTo("It's raining");
    assertThat(viaMessageSource).isEqualTo(direct);
  }
}
