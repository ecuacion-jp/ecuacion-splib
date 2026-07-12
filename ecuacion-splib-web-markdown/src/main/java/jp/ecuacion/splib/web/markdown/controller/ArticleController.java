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
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.markdown.service.ArticleService;
import org.slf4j.event.Level;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Controller for Markdown-based article pages. */
@Controller
public class ArticleController {

  private final ArticleService articleService;

  /** Constructor. */
  public ArticleController(ArticleService articleService) {
    this.articleService = articleService;
  }

  /**
   * Shows the article page for the given article ID, in the requested language.
   *
   * @param id    the article identifier
   * @param lang  optional language tag (e.g. {@code "ja"}) overriding the resolved locale; when
   *              absent, the locale resolved by the configured {@code LocaleResolver} is used
   * @param model the Spring MVC model
   * @return template name
   * @throws RedirectToHomePageException if {@code id} is malformed, or no article is found for
   *     it, not even in the default language
   */
  @GetMapping("/public/showMarkdown/page")
  public String showArticle(
      @RequestParam String id, @RequestParam(required = false) String lang, Model model) {

    // Validate input string to prevent from attacks.
    if (!ArticleService.ID_PATTERN.matcher(id).matches()) {
      throw new RedirectToHomePageException(Level.INFO, "ARTICLE_NOT_FOUND_MSG",
          new String[] {id});
    }

    Locale locale = lang != null ? Locale.forLanguageTag(lang) : LocaleContextHolder.getLocale();
    String content = articleService.renderArticle(locale, id)
        .orElseThrow(() -> new RedirectToHomePageException(Level.INFO, "ARTICLE_NOT_FOUND_MSG",
            new String[] {locale + "/" + id}));

    model.addAttribute("content", content);
    model.addAttribute("currentArticleId", id);

    // "markdownPage.{id}.title" drives the page-base title area; when absent, the title area
    // stays empty and the Markdown's own heading (if any) is the only title shown.
    // Resolved eagerly with the same locale as the article content itself (rather than left
    // as a key for Thymeleaf to resolve against the ambient session locale), so the title
    // stays in sync with the content even when it is requested in a locale other than the
    // one currently active in the session (e.g. via an explicit lang= override).
    String titleKey = "markdownPage." + id + ".title";
    if (PropertiesFileUtil.hasMessage(titleKey)) {
      model.addAttribute("title", PropertiesFileUtil.getMessage(locale, titleKey));
    }

    return "article";
  }
}
