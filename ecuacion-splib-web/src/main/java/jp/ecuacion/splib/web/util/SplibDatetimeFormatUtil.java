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

import jakarta.servlet.http.HttpServletRequest;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;

/**
 * Provides utility methods to build {@link DatetimeFormatParameters} from a request.
 */
public class SplibDatetimeFormatUtil {

  private SplibDatetimeFormatUtil() {}

  /**
   * Returns DatetimeFormatParameters built from the {@code zoneOffset} session attribute.
   *
   * @param request request
   * @return DatetimeFormatParameters
   */
  public static DatetimeFormatParameters getParams(HttpServletRequest request) {
    DatetimeFormatParameters params = new DatetimeFormatParameters();
    String strOffset = (String) request.getSession().getAttribute("zoneOffset");

    // strOffset can be null due to irregular behavior,
    // such as when a system error occurs and the user is redirected to the login screen
    // even though userDetails are still in the session.
    // In that case the subsequent processing would fail with a null argument,
    // so set it to UTC (0) when null.
    // This should be fine since time is unlikely to be displayed on non-login screens.
    if (strOffset == null) {
      strOffset = "0";
    }

    params.setZoneOffsetWithJsMinutes(Integer.parseInt(strOffset));

    return params;
  }
}
