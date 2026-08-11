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

import jp.ecuacion.splib.core.util.SplibPropertiesCacheClearer;
import jp.ecuacion.splib.web.controller.ConfigController.ConfigForm;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.record.ConfigRecord;
import jp.ecuacion.splib.web.service.SplibGeneral1FormDoNothingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Shows the prepared ecuacion config page.
 */
@Controller
@Scope("prototype")
@RequestMapping(ConfigController.BASE_PATH)
public class ConfigController
    extends SplibGeneral1FormController<ConfigForm, SplibGeneral1FormDoNothingService<ConfigForm>> {

  /**
   * Note: this controller's URL doesn't follow the {@code /{loginState}/{function}} convention
   * that {@code ReturnUrlBuilder} assumes (it would turn this into
   * {@code /ecuacion-splib-admin/...}, which doesn't match this mapping), so redirects here
   * are built from this constant directly instead of via {@code getRedirectUrlOnSuccess()}.
   *
   * <p>Mapped under {@code /ecuacion-splib/admin/**}, so it requires login via
   * {@code ecuacion-splib}'s own built-in admin login; see
   * {@code SplibBuiltinAdminSecurityConfig}. This controller exposes side-effecting actions
   * ({@link #clearPropertiesCache()}, {@link #systemError()}), so — unlike
   * {@code /ecuacion-splib/public/**}, which is {@code permitAll} — it must not be reachable
   * without authentication.</p>
   */
  static final String BASE_PATH = "/ecuacion-splib/admin/config";

  @Autowired
  private SplibPropertiesCacheClearer propertiesCacheClearer;

  /**
   * Constructs a new instance.
   */
  public ConfigController() {
    super("config");
  }

  /**
   * Clears the cache of properties files read via {@code PropertiesFileUtil} (and, when
   * available, spring's own properties cache), so that changes to application.properties can
   * be picked up without restarting the app.
   *
   * @return URL
   */
  @PostMapping(value = "action", params = "action=clearPropertiesCache")
  public String clearPropertiesCache() {
    propertiesCacheClearer.clear();

    return "redirect:" + BASE_PATH + "/page?success";
  }

  /**
   * Deliberately throws a system error so that the system error behavior
   * (error page, logging, and so on) can be tested without requiring an actual bug.
   *
   * @return never returns
   */
  @PostMapping(value = "action", params = "action=systemError")
  public String systemError() {
    throw new RuntimeException("A system error was intentionally caused for testing purposes.");
  }

  /**
   * Stores data for config.
   */
  public static class ConfigForm extends SplibGeneralForm {

    private ConfigRecord config = new ConfigRecord();

    public ConfigRecord getConfig() {
      return config;
    }

    public void setConfig(ConfigRecord config) {
      this.config = config;
    }
  }
}
