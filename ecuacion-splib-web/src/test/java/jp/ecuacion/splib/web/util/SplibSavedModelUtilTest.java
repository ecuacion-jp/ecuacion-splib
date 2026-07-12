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

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

/**
 * Unit tests for {@link SplibSavedModelUtil#saveToFlash}.
 */
class SplibSavedModelUtilTest {

  /**
   * Regression test: {@code KEY_GLOBAL_ERRORS} must never be part of the snapshot, even when
   * {@code takeOverMessages} is {@code true}.
   *
   * <p>{@code globalErrors} is recomputed from scratch on every request by
   * {@code SplibControllerAdvice#aggregateGlobalErrors}. If the value present in {@code model}
   * at snapshot time (typically the empty list set at the start of the very request that is
   * now redirecting) were carried over, it would overwrite a message a caller flashes directly
   * under the same key on the redirect target
   * (e.g. {@code SplibExceptionHandler#handleRedirectNeededExceptions}).</p>
   */
  @Test
  void saveToFlash_neverCarriesOverGlobalErrors_evenWhenTakeOverMessagesIsTrue() {
    Model model = new ExtendedModelMap();
    model.addAttribute(SplibWebConstants.KEY_GLOBAL_ERRORS, List.of("stale error"));

    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    SplibSavedModelUtil.saveToFlash(model, redirectAttributes, true);

    @SuppressWarnings("unchecked")
    var snapshot =
        (java.util.Map<String, Object>) redirectAttributes.getFlashAttributes()
            .get(SplibWebConstants.KEY_SAVED_MODEL);
    assertThat(snapshot).doesNotContainKey(SplibWebConstants.KEY_GLOBAL_ERRORS);
  }

  @Test
  void saveToFlash_carriesOverOtherAttributes_whenTakeOverMessagesIsTrue() {
    Model model = new ExtendedModelMap();
    model.addAttribute("someForm", "formValue");
    model.addAttribute(SplibWebConstants.KEY_WARN_MESSAGE, "warn");

    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    SplibSavedModelUtil.saveToFlash(model, redirectAttributes, true);

    @SuppressWarnings("unchecked")
    var snapshot =
        (java.util.Map<String, Object>) redirectAttributes.getFlashAttributes()
            .get(SplibWebConstants.KEY_SAVED_MODEL);
    assertThat(snapshot).containsEntry("someForm", "formValue");
    assertThat(snapshot).containsEntry(SplibWebConstants.KEY_WARN_MESSAGE, "warn");
  }

  @Test
  void saveToFlash_stripsWarnAndSuccessAndBindingResult_whenTakeOverMessagesIsFalse() {
    Model model = new ExtendedModelMap();
    model.addAttribute(SplibWebConstants.KEY_WARN_MESSAGE, "warn");
    model.addAttribute(SplibWebConstants.KEY_NEEDS_SUCCESS_MESSAGE, true);
    model.addAttribute("someForm", "formValue");

    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    SplibSavedModelUtil.saveToFlash(model, redirectAttributes, false);

    @SuppressWarnings("unchecked")
    var snapshot =
        (java.util.Map<String, Object>) redirectAttributes.getFlashAttributes()
            .get(SplibWebConstants.KEY_SAVED_MODEL);
    assertThat(snapshot).doesNotContainKey(SplibWebConstants.KEY_WARN_MESSAGE);
    assertThat(snapshot).doesNotContainKey(SplibWebConstants.KEY_NEEDS_SUCCESS_MESSAGE);
    assertThat(snapshot).containsEntry("someForm", "formValue");
  }
}
