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

package jp.ecuacion.splib.web.markdown.controller;

import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Adds the resolved language code ({@code ja} or {@code en}) to every model
 * so Thymeleaf templates can build language-aware URLs.
 *
 * <p>The locale is resolved by the configured {@code CookieLocaleResolver}:
 * Cookie {@code lang} → Accept-Language header → English fallback.</p>
 */
@ControllerAdvice
public class LangAdvice {

  /**
   * Returns {@code "ja"} when the resolved locale is Japanese; {@code "en"} otherwise.
   *
   * @return language code string
   */
  @ModelAttribute("lang")
  public String resolveLang() {
    Locale locale = LocaleContextHolder.getLocale();
    return Locale.JAPANESE.getLanguage().equals(locale.getLanguage()) ? "ja" : "en";
  }
}
