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

import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.springframework.stereotype.Component;

/**
 * Builds a {@code <style>} block overriding bootstrap theme color css variables
 *     (ex. {@code --bs-color-navbar}, {@code --bs-color-navbar-rgb})
 *     with values from application[...].properties, without rebuilding bootstrap from sass.
 *
 * <p>Each app's bootstrap.min.css exposes its theme colors as css variables scoped to
 *     the {@code [data-bs-theme="light"]} selector, and every utility class
 *     (ex. {@code .bg-color-navbar}) reads them via {@code var(...)}.
 *     Outputting the same selector again after the app's own bootstrap.min.css is loaded
 *     lets the values specified here win, purely by css cascade order.</p>
 *
 * <p>It's supposed to be called from thymeleaf (ex. {@code bootstrap/page-base}).</p>
 */
@Component("themeColorUtil")
public class SplibThemeColorUtil {

  private static final String KEY_PREFIX = "jp.ecuacion.splib.web.theme.color-";

  private static final String[] SUFFIXES = {"navbar", "subtitle", "btn", "btn-danger", "footer",
      "paging-btn", "tbl-header", "msg-success-bg", "msg-error-bg"};

  /**
   * Constructs a new instance.
   */
  public SplibThemeColorUtil() {}

  /**
   * Returns whether any of the theme color keys exists in application[...].properties.
   *
   * @return boolean
   */
  public boolean hasAnyThemeColor() {
    for (String suffix : SUFFIXES) {
      if (PropertiesFileUtil.hasApplication(KEY_PREFIX + suffix)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Builds the css string to override theme color variables,
   *     using only the keys actually set in application[...].properties.
   * Returns an empty string when none of the keys are set.
   *
   * @return css string. ex. {@code [data-bs-theme="light"]{--bs-color-navbar:#4fa7ff;
   *     --bs-color-navbar-rgb:79,167,255;}}
   */
  public String buildThemeColorCssOrEmpty() {
    StringBuilder sb = new StringBuilder();

    for (String suffix : SUFFIXES) {
      String key = KEY_PREFIX + suffix;
      if (!PropertiesFileUtil.hasApplication(key)) {
        continue;
      }

      String hex = PropertiesFileUtil.getApplication(key);
      String rgb = hexToRgb(hex);
      sb.append("--bs-color-").append(suffix).append(':').append(hex).append(';');
      sb.append("--bs-color-").append(suffix).append("-rgb:").append(rgb).append(';');
    }

    return sb.length() == 0 ? "" : "[data-bs-theme=\"light\"]{" + sb + "}";
  }

  /**
   * Converts a {@code #rrggbb} hex color string into a css rgb triplet (ex. {@code "79,167,255"}).
   * Only the 6-digit {@code #rrggbb} format is supported.
   */
  private String hexToRgb(String hex) {
    String h = hex.startsWith("#") ? hex.substring(1) : hex;
    int r = Integer.parseInt(h.substring(0, 2), 16);
    int g = Integer.parseInt(h.substring(2, 4), 16);
    int b = Integer.parseInt(h.substring(4, 6), 16);

    return r + "," + g + "," + b;
  }
}
