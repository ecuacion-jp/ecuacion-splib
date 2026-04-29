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
import java.util.Objects;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.context.support.AbstractMessageSource;

/**
 * A {@link org.springframework.context.MessageSource} implementation
 * that delegates to {@link PropertiesFileUtil}.
 *
 * <p>Supports type-aware {@link MessageFormat} formatting
 * (e.g., {@code {0,date,yyyy/MM/dd}}, {@code {0,number,#,###}})
 * via the locale-aware {@link MessageFormat} returned from {@link #resolveCode}.</p>
 *
 * <p>When a key is not found in {@link PropertiesFileUtil},
 * resolution falls through to the parent {@link org.springframework.context.MessageSource}
 * (configured via {@link #setParentMessageSource}) if one is set.</p>
 */
public class PropertiesFileUtilMessageSource extends AbstractMessageSource {

  /**
   * Constructs a new instance.
   */
  public PropertiesFileUtilMessageSource() {}

  /**
   * Resolves the given message code via {@link PropertiesFileUtil}.
   *
   * <p>Returns {@code null} when the key does not exist,
   * allowing the parent {@link org.springframework.context.MessageSource} to handle it.</p>
   *
   * @param code the message code to resolve
   * @param locale the locale to resolve the code for
   * @return a {@link MessageFormat} for the resolved message, or {@code null} if not found
   */
  @Override
  protected @Nullable MessageFormat resolveCode(@Nullable String code, @Nullable Locale locale) {
    if (!PropertiesFileUtil.hasMessage(Objects.requireNonNull(code))) {
      return null;
    }
    
    String template = PropertiesFileUtil.getMessage(locale, code);
    return new MessageFormat(template, locale);
  }
}
