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
import java.util.Arrays;
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
   * Returns HTML rendered from {@code classpath:content/{id}_{lang}.md}.
   *
   * @param lang the language code ({@code "ja"} or {@code "en"})
   * @param id   the article identifier; only alphanumerics, hyphens, and slashes are allowed
   * @return rendered HTML string
   * @throws IllegalArgumentException if {@code id} contains invalid characters or the file is
   *     not found
   */
  public String renderArticle(String lang, String id) {
    if (!id.matches("[a-zA-Z0-9][a-zA-Z0-9\\-]*(/[a-zA-Z0-9][a-zA-Z0-9\\-]*)*")) {
      throw new IllegalArgumentException("Invalid article id: " + id);
    }
    String resourcePath = "content/" + id + "_" + lang + ".md";
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Article not found: " + lang + "/" + id);
      }
      String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      Node document = parser.parse(markdown);
      return renderer.render(document);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read article: " + lang + "/" + id, ex);
    }
  }
}
