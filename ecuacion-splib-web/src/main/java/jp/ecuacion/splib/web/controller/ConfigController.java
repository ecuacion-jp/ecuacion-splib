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

import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.web.controller.ConfigController.ConfigForm;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.record.ConfigRecord;
import jp.ecuacion.splib.web.service.SplibGeneral1FormDoNothingService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Shows the prepared ecuacion config page.
 */
@Controller
@Scope("prototype")
@RequestMapping("/ecuacion/public/config")
public class ConfigController
    extends SplibGeneral1FormController<ConfigForm, SplibGeneral1FormDoNothingService<ConfigForm>> {

  /**
   * Constructs a new instance.
   */
  public ConfigController() {
    super("config");
  }

  /**
   * Clears the cache of properties files read via {@code PropertiesFileUtil},
   * so that changes to application.properties can be picked up without restarting the app.
   *
   * <p>Rejected unless {@code jp.ecuacion.splib.web.ecuacion-config-buttons.enabled}
   *     is set to {@code true} in application.properties.</p>
   *
   * @return URL
   */
  @PostMapping(value = "action", params = "action=clearPropertiesCache")
  public String clearPropertiesCache() {
    checkConfigButtonsEnabled();

    PropertiesFileUtil.clearCache();

    return getRedirectUrlOnSuccess();
  }

  /**
   * Deliberately throws a system error so that the system error behavior
   * (error page, logging, and so on) can be tested without requiring an actual bug.
   *
   * <p>Rejected unless {@code jp.ecuacion.splib.web.ecuacion-config-buttons.enabled}
   *     is set to {@code true} in application.properties.</p>
   *
   * @return never returns
   */
  @PostMapping(value = "action", params = "action=systemError")
  public String systemError() {
    checkConfigButtonsEnabled();

    throw new RuntimeException("A system error was intentionally caused for testing purposes.");
  }

  private void checkConfigButtonsEnabled() {
    if (!Boolean.parseBoolean(PropertiesFileUtil
        .getApplicationOrElse("jp.ecuacion.splib.web.ecuacion-config-buttons.enabled", "false"))) {
      throw new RedirectToHomePageException("jp.ecuacion.splib.web.common.message.urlNotProper");
    }
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
