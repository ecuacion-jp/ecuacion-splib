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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives zone offset value from the user PC and set it into the session.
 */
@RestController
public class ZoneOffsetSettingRestController {

  @Autowired
  private HttpServletRequest request;
  
  /**
   * Receives zone offset value from the user PC and set it into the session.
   * 
   * @param zoneOffset zoneOffset
   */
  @GetMapping("/public/zoneOffset")
  public void zoneOffset(@RequestParam String zoneOffset) {
    request.getSession().setAttribute("zoneOffset", zoneOffset);
  }
}
