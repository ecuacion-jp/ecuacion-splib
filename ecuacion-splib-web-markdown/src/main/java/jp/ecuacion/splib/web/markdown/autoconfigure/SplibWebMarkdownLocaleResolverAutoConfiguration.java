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

package jp.ecuacion.splib.web.markdown.autoconfigure;

import jp.ecuacion.splib.web.markdown.config.SplibWebMarkdownConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Auto-configures the session-based {@link LocaleResolver} that the language switch of
 * splib-web-markdown relies on, unless the application defines its own.
 *
 * <p>An application can replace it by defining a {@link LocaleResolver} bean named
 *     {@code localeResolver} (e.g. one that returns the language a user chose on a profile
 *     page).</p>
 *
 * <p>This is an auto-configuration rather than part of {@link SplibWebMarkdownConfig} so that
 *     the application's bean is always registered before the condition is evaluated. For the
 *     same reason it is placed outside the {@code config} package, which applications
 *     component-scan. It runs before {@link WebMvcAutoConfiguration} so that Spring Boot's
 *     default {@code AcceptHeaderLocaleResolver} yields to this one.</p>
 */
@AutoConfiguration(before = WebMvcAutoConfiguration.class)
public class SplibWebMarkdownLocaleResolverAutoConfiguration {

  /**
   * Session-based locale resolver.
   * Falls back to the browser's {@code Accept-Language} header when no locale is
   * set in the session yet.
   */
  @Bean
  @ConditionalOnMissingBean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
  LocaleResolver localeResolver() {
    return new SessionLocaleResolver();
  }
}
