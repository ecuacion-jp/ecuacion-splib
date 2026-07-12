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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import jp.ecuacion.splib.web.markdown.service.MarkdownPageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

/** Switches the display language and returns to the Markdown page the user was viewing. */
@Controller
@RequestMapping("/public/lang")
public class LangController {

  private final LocaleResolver localeResolver;

  /** Constructor. */
  public LangController(LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  /**
   * Switches the display language by updating the locale via the configured
   * {@link LocaleResolver}, then redirects back to the Markdown page identified by
   * {@code returnId} (or the home page when absent or invalid).
   *
   * @param lang     {@code ja} or {@code en}
   * @param returnId the Markdown page identifier to return to
   * @param request  HTTP request
   * @param response HTTP response
   * @return redirect to the Markdown page in the selected language
   */
  @GetMapping("/{lang}")
  public String switchLang(
      @PathVariable String lang,
      @RequestParam(defaultValue = "home") String returnId,
      HttpServletRequest request,
      HttpServletResponse response) {
    String safeLang = "ja".equals(lang) ? "ja" : "en";
    Locale locale = "ja".equals(safeLang) ? Locale.JAPANESE : Locale.ENGLISH;
    localeResolver.setLocale(request, response, locale);
    String safeId = MarkdownPageService.ID_PATTERN.matcher(returnId).matches() ? returnId : "home";
    return "redirect:/public/showMarkdown/page?id=" + safeId + "&lang=" + safeLang;
  }
}
