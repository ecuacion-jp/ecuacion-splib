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

package jp.ecuacion.splib.web.markdown.config;

import java.time.Duration;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

/** Spring configuration for tutorial-app language resolution. */
@Configuration
public class LocaleConfig {

  /**
   * Cookie-based locale resolver using the {@code lang} cookie.
   * Falls back to English for unrecognised locales.
   */
  @Bean
  LocaleResolver localeResolver() {
    CookieLocaleResolver resolver = new CookieLocaleResolver("lang");
    resolver.setDefaultLocale(Locale.ENGLISH);
    resolver.setCookieMaxAge(Duration.ofDays(365));
    return resolver;
  }
}
