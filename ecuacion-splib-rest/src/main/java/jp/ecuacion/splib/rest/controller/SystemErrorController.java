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

import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.rest.exception.HttpStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides a controller that deliberately causes a system error.
 *
 * <p>Mapped under {@code /api/ecuacion/public/**}; see {@code AliveCheckController}.</p>
 */
@RestController
public class SystemErrorController {

  /**
   * Deliberately throws a system error so that the system error behavior
   * (exception handling, logging, and so on) can be tested without requiring an actual bug.
   *
   * <p>Rejected unless {@code jp.ecuacion.splib.rest.ecuacion-config-endpoints.enabled}
   *     is set to {@code true} in application.properties.</p>
   *
   * @throws HttpStatusException if the endpoint is not enabled
   */
  @PostMapping("/api/ecuacion/public/systemError")
  public void systemError() throws HttpStatusException {
    checkConfigEndpointsEnabled();

    throw new RuntimeException("A system error was intentionally caused for testing purposes.");
  }

  private void checkConfigEndpointsEnabled() throws HttpStatusException {
    if (!Boolean.parseBoolean(PropertiesFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.rest.ecuacion-config-endpoints.enabled", "false"))) {
      throw new HttpStatusException(HttpStatus.FORBIDDEN);
    }
  }
}
