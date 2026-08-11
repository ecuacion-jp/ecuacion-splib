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

import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.core.util.SplibPropertiesCacheClearer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides a controller that clears the {@code PropertiesFileUtil} cache.
 *
 * <p>Mapped under {@code /api/ecuacion-splib/key/**}, so it requires {@code X-Api-Key}
 *     authentication; see {@code SplibBuiltinApiKeyAuthenticationFilter}.</p>
 */
@RestController
public class ClearPropertiesCacheController {

  private final DetailLogger detailLog = new DetailLogger(this);

  @Autowired
  private SplibPropertiesCacheClearer propertiesCacheClearer;

  /**
   * Clears the cache of properties files read via {@code PropertiesFileUtil} (and, when
   * available, spring's own properties cache), so that changes to application.properties can
   * be picked up without restarting the app.
   */
  @PostMapping("/api/ecuacion-splib/key/clearPropertiesCache")
  public void clearPropertiesCache() {
    propertiesCacheClearer.clear();
    detailLog.info("PropertiesFileUtil cache was cleared via clearPropertiesCache API.");
  }
}
