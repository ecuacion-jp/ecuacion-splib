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

package jp.ecuacion.splib.web.markdown.service;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Reads Markdown files from classpath and renders them as HTML. */
@Service
public class ArticleService {

  private final Parser parser;
  private final HtmlRenderer renderer;

  /** Initializes the Markdown parser with GFM-compatible extensions. */
  public ArticleService() {
    MutableDataSet options = new MutableDataSet();
    options.set(Parser.EXTENSIONS, Arrays.asList(
        TablesExtension.create(),
        StrikethroughExtension.create()
    ));
    parser = Parser.builder(options).build();
    renderer = HtmlRenderer.builder(options).build();
  }

  /**
   * Returns HTML rendered from the Markdown file resolved for {@code locale}, falling back
   * through less specific variants the same way {@link java.util.ResourceBundle} resolves
   * property files: {@code content/{id}_{language}_{country}_{variant}.md} →
   * {@code content/{id}_{language}_{country}.md} → {@code content/{id}_{language}.md} →
   * {@code content/{id}.md} (the root/default article, with no suffix).
   *
   * @param locale the requested locale
   * @param id     the article identifier; only alphanumerics, hyphens, and slashes are allowed
   * @return rendered HTML string
   * @throws IllegalArgumentException if {@code id} contains invalid characters or no file is
   *     found for {@code id}, not even the root article
   */
  public String renderArticle(Locale locale, String id) {
    if (!id.matches("[a-zA-Z0-9][a-zA-Z0-9\\-]*(/[a-zA-Z0-9][a-zA-Z0-9\\-]*)*")) {
      throw new IllegalArgumentException("Invalid article id: " + id);
    }

    for (String suffix : candidateSuffixes(locale)) {
      String resourcePath = "content/" + id + suffix + ".md";
      try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
        if (is == null) {
          continue;
        }
        String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Node document = parser.parse(markdown);
        return renderer.render(document);
      } catch (IOException ex) {
        throw new IllegalStateException("Failed to read article: " + resourcePath, ex);
      }
    }

    throw new IllegalArgumentException("Article not found: " + locale + "/" + id);
  }

  /**
   * Builds the candidate filename suffixes for {@code locale}, most specific first and ending
   * with {@code ""} for the root/default article, mirroring
   * {@code ResourceBundle.Control#getCandidateLocales}.
   */
  private List<String> candidateSuffixes(Locale locale) {
    List<String> suffixes = new ArrayList<>();
    String language = locale.getLanguage();
    String country = locale.getCountry();
    String variant = locale.getVariant();

    if (!variant.isEmpty() && !country.isEmpty() && !language.isEmpty()) {
      suffixes.add("_" + language + "_" + country + "_" + variant);
    }
    if (!country.isEmpty() && !language.isEmpty()) {
      suffixes.add("_" + language + "_" + country);
    }
    if (!language.isEmpty()) {
      suffixes.add("_" + language);
    }
    suffixes.add("");
    return suffixes;
  }
}
