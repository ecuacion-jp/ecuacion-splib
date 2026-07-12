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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.springframework.stereotype.Service;

/** Reads Markdown files from classpath and renders them as HTML. */
@Service
public class MarkdownPageService {

  /** Only alphanumerics, hyphens, and slashes are allowed in a Markdown page id. */
  public static final Pattern ID_PATTERN =
      Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9\\-]*(/[a-zA-Z0-9][a-zA-Z0-9\\-]*)*");

  private final DetailLogger detailLog = new DetailLogger(this);

  private final Parser parser;
  private final HtmlRenderer renderer;

  /** Initializes the Markdown parser with GFM-compatible extensions. */
  public MarkdownPageService() {
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
   * property files: {@code markdown/{id}_{language}_{country}_{variant}.md} →
   * {@code markdown/{id}_{language}_{country}.md} → {@code markdown/{id}_{language}.md} →
   * {@code markdown/{id}.md} (the root/default page, with no suffix).
   *
   * <p>Assumes {@code id} has already been validated by the caller; it is trusted here and
   * used as-is to build the classpath lookup.</p>
   *
   * @param locale the requested locale
   * @param id     the Markdown page identifier
   * @return the rendered HTML, or {@link Optional#empty()} if no file is found for {@code id},
   *     not even the root page
   * @throws UncheckedIOException if a matching Markdown file exists but cannot be read
   */
  public Optional<String> renderMarkdownPage(Locale locale, String id) {
    List<String> suffixes = candidateSuffixes(locale);
    for (int i = 0; i < suffixes.size(); i++) {
      String resourcePath = "markdown/" + id + suffixes.get(i) + ".md";
      try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
        if (is == null) {
          continue;
        }
        if (i > 0) {
          detailLog.debug("Markdown page locale fallback: requested locale '" + locale
              + "' has no '" + suffixes.get(0) + "' variant for id '" + id
              + "'; using '" + resourcePath + "' instead.");
        }
        String markdown = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Node document = parser.parse(markdown);
        return Optional.of(renderer.render(document));
      } catch (IOException ex) {
        throw new UncheckedIOException("Failed to read Markdown page: " + resourcePath, ex);
      }
    }

    return Optional.empty();
  }

  /**
   * Builds the candidate filename suffixes for {@code locale}, most specific first and ending
   * with {@code ""} for the root/default page, mirroring
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
