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

import java.text.MessageFormat;
import java.util.Locale;
import jp.ecuacion.lib.core.util.LocaleUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.context.support.AbstractMessageSource;

/**
 * A {@link org.springframework.context.MessageSource} implementation
 * that delegates to {@link PropertiesFileUtil}.
 *
 * <p>Key resolution priority is: {@code messages_*.properties} first,
 * then {@code constants_*.properties}, then {@code item_names_*.properties}.
 * Returns {@code null} when a key is not found in any of them.</p>
 *
 * <p>Supports type-aware {@link MessageFormat} formatting
 * (e.g., {@code {0,date,yyyy/MM/dd}}, {@code {0,number,#,###}})
 * via the locale-aware {@link MessageFormat} returned from {@link #resolveCode}.</p>
 */
public class PropertiesFileUtilMessageSource extends AbstractMessageSource {

  /**
   * Constructs a new instance.
   */
  public PropertiesFileUtilMessageSource() {
    setUseCodeAsDefaultMessage(true);
  }

  /**
   * Resolves the given message code via {@link PropertiesFileUtil}.
   *
   * <p>Searches in order: {@code messages}, {@code constants}, {@code item_names}.
   * Returns {@code null} when the key does not exist in any file.</p>
   *
   * @param code the message code to resolve
   * @param locale the locale to resolve the code for
   * @return a {@link MessageFormat} for the resolved message, or {@code null} if not found
   */
  @Override
  protected @Nullable MessageFormat resolveCode(String code, @Nullable Locale locale) {
    String template;
    if (PropertiesFileUtil.hasMessage(code)) {
      template = PropertiesFileUtil.getMessage(locale, code);
    } else if (PropertiesFileUtil.hasConstant(code)) {
      template = PropertiesFileUtil.getConstant(code);
    } else if (PropertiesFileUtil.hasItemName(code)) {
      template = PropertiesFileUtil.getItemName(locale, code);
    } else {
      return null;
    }
    return new MessageFormat(template, locale != null ? locale : LocaleUtil.getFallbackLocale());
  }

  /**
   * Disables {@code useCodeAsDefaultMessage} for Spring's own {@code "problemDetail."}-prefixed
   * codes (e.g. {@code problemDetail.org.springframework.web.server.ResponseStatusException}).
   *
   * <p>Spring's {@code ErrorResponse.updateAndGetBody} resolves these codes to fill in a
   * {@code ProblemDetail}'s {@code detail}/{@code title}, but only overwrites them when the
   * {@link org.springframework.context.MessageSource} actually returns a value. With
   * {@code useCodeAsDefaultMessage} left on for these codes — which this application's
   * {@code messages}/{@code constants}/{@code item_names} files were never meant to translate —
   * the raw, unresolved code was echoed back as if it were a real message, silently discarding
   * whatever specific reason/message the throwing code had set on the
   * {@code org.springframework.web.server.ResponseStatusException} (or other
   * {@code org.springframework.web.ErrorResponse}). Returning {@code null} here instead lets
   * Spring fall back to that original reason/message.</p>
   */
  @Override
  protected @Nullable String getDefaultMessage(String code) {
    if (code.startsWith("problemDetail.")) {
      return null;
    }

    return super.getDefaultMessage(code);
  }
}
