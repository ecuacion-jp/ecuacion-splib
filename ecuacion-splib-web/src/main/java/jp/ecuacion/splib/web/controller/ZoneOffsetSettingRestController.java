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
package jp.ecuacion.splib.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.DateTimeException;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives zone offset value from the user PC and set it into the session.
 */
@RestController
public class ZoneOffsetSettingRestController {

  private HttpServletRequest request;

  /**
   * Constructs a new instance.
   *
   * @param request request
   */
  public ZoneOffsetSettingRestController(HttpServletRequest request) {
    this.request = request;
  }
  
  /**
   * Receives zone offset value from the user PC and set it into the session.
   *
   * <p>{@code zoneOffset} is silently ignored (the session is left unchanged) when it is not a
   *     valid integer, or when it is out of the range {@link java.time.ZoneOffset} supports
   *     (-18:00 to +18:00), rather than storing it as-is and letting {@link
   *     jp.ecuacion.splib.web.util.SplibDatetimeFormatUtil#getParams} fail later with a
   *     {@code NumberFormatException} or a {@code DateTimeException}
   *     when the value is finally used.</p>
   *
   * @param zoneOffset zoneOffset
   */
  @GetMapping("/public/zoneOffset")
  public void zoneOffset(@RequestParam String zoneOffset) {
    try {
      // Converts it the same way as SplibDatetimeFormatUtil#getParams does,
      // so that only the values accepted there are stored.
      new DatetimeFormatParameters().setZoneOffsetWithJsMinutes(Integer.parseInt(zoneOffset));

    } catch (NumberFormatException | DateTimeException ex) {
      return;
    }

    request.getSession().setAttribute("zoneOffset", zoneOffset);
  }
}
