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
package jp.ecuacion.splib.web.form;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.internal.PropertiesFileUtilBundleReader;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.ui.constant.SplibUiConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the messages of ecuacion-splib's own required-field ({@code notEmpty}) check.
 * The check raises a {@link BusinessViolation}, whose item name placeholder is
 * {@code {item_name}}, not {@code {0}}.
 */
class SplibNotEmptyMessageTest {

  @BeforeAll
  static void init() {
    PropertiesFileUtilBundleReader.addToDynamicPostfixList("splib-web-test");
  }

  private static String message(Locale locale) {
    return message(locale, true);
  }

  private static String message(Locale locale, boolean isMessageWithItemName) {
    Violations violations = new Violations().add(new BusinessViolation(
        new String[] {"testRecord.name"}, new String[] {"name"},
        SplibUiConstants.MESSAGE_KEY_NOT_EMPTY));
    List<String> messages =
        ExceptionUtil.getMessageList(violations, locale, isMessageWithItemName);
    assertThat(messages).hasSize(1);
    return messages.get(0);
  }

  @Test
  void english_itemNameIsEmbedded() {
    assertThat(message(Locale.ENGLISH)).contains("Test name").endsWith("is required")
        .doesNotContain("{");
  }

  @Test
  void japanese_itemNameIsEmbedded() {
    assertThat(message(Locale.JAPANESE)).contains("Test name").endsWith("は入力必須です")
        .doesNotContain("{");
  }

  // Message without the item name (shown next to the item). ecuacion-splib-web does not depend
  // on ecuacion-lib-validation-business-messages, so these come from
  // ValidationMessages_splib_web[_ja].properties.

  @Test
  void english_withoutItemName_fromSplibWebValidationMessages() {
    assertThat(message(Locale.ENGLISH, false)).isEqualTo("must not be empty");
  }

  @Test
  void japanese_withoutItemName_fromSplibWebValidationMessages() {
    assertThat(message(Locale.JAPANESE, false)).isEqualTo("空要素は許可されていません");
  }
}
