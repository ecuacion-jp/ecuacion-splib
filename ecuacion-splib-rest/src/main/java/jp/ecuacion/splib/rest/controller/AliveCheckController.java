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
package jp.ecuacion.splib.rest.controller;

import jp.ecuacion.splib.rest.dto.StatusResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides an alive-check controller.
 *
 * <p>Mapped under {@code /api/ecuacion/public/**}, not {@code /api/public/**} — the latter is
 *     reserved for the application's own endpoints; see
 *     {@code SplibRestSecurityConfig#filterChainForApiPublic}.</p>
 */
@RestController
public class AliveCheckController {

  /**
   * Provides an alive-check response.
   *
   * @return a {@code StatusResponse} with {@code "OK"}
   */
  @RequestMapping(value = "/api/ecuacion/public/aliveCheck",
      method = {RequestMethod.GET, RequestMethod.POST})
  public StatusResponse aliveCheck() {
    return new StatusResponse("OK");
  }
}
