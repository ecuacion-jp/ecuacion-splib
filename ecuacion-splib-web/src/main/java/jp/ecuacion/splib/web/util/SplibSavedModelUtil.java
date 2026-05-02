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
package jp.ecuacion.splib.web.util;

import java.util.HashMap;
import java.util.Map;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Saves the current {@code Model} into flash attributes so it can be restored on the
 * redirect target page (paired with {@code SplibControllerAdvice} which restores it).
 */
public class SplibSavedModelUtil {

  private SplibSavedModelUtil() {}

  /**
   * Saves a snapshot of {@code model} into {@code redirectAttributes} as a flash attribute
   * keyed by {@link SplibWebConstants#KEY_SAVED_MODEL}.
   *
   * <p>When {@code takeOverMessages} is {@code false}, warning / success-flag /
   *     {@code BindingResult}-prefixed entries are stripped from the snapshot so the
   *     redirected page starts with a clean error / warning state.</p>
   *
   * @param model model
   * @param redirectAttributes redirectAttributes
   * @param takeOverMessages whether to carry over warning / error / success messages
   */
  public static void saveToFlash(Model model, RedirectAttributes redirectAttributes,
      boolean takeOverMessages) {

    Map<String, Object> modelSnapshot = new HashMap<>(model.asMap());

    if (!takeOverMessages) {
      modelSnapshot.remove(SplibWebConstants.KEY_WARN_MESSAGE);
      modelSnapshot.remove(SplibWebConstants.KEY_NEEDS_SUCCESS_MESSAGE);
      modelSnapshot.entrySet()
          .removeIf(entry -> entry.getKey().startsWith(BindingResult.MODEL_KEY_PREFIX));
    }

    redirectAttributes.addFlashAttribute(SplibWebConstants.KEY_SAVED_MODEL, modelSnapshot);
  }
}
