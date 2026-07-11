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
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Redirects to the language-appropriate home page based on the resolved locale. */
@Controller
@RequestMapping("/public/localeHome")
public class HomeRedirectController {

  /**
   * Resolves the current language from the locale context and redirects to the home article.
   *
   * <p>Flash attributes (e.g. {@code globalErrors} set by a preceding
   * {@code RedirectToHomePageException}) only survive a single redirect hop, and this
   * controller itself issues a second redirect (to the language-specific home article), so
   * any such message must be re-flashed here or it is silently dropped.</p>
   *
   * @param model model, already populated with flash attributes carried over from the
   *     previous redirect
   * @param redirectAttributes used to re-flash the message across this redirect
   * @return redirect URL
   */
  @GetMapping
  public String home(Model model, RedirectAttributes redirectAttributes) {
    Locale locale = LocaleContextHolder.getLocale();
    String lang = Locale.JAPANESE.getLanguage().equals(locale.getLanguage()) ? "ja" : "en";

    Object globalErrors = model.getAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS);
    if (globalErrors != null) {
      redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS, globalErrors);
    }

    return "redirect:/public/showMarkdown/page?id=home&lang=" + lang;
  }
}
