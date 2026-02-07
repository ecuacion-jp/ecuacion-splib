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

import jp.ecuacion.splib.web.controller.ConfigController.ConfigForm;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.record.ConfigRecord;
import jp.ecuacion.splib.web.service.SplibGeneral1FormDoNothingService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
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
