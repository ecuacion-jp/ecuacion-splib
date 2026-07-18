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
package jp.ecuacion.splib.web.record;

import jp.ecuacion.lib.core.annotation.ItemNameKeyClass;
import jp.ecuacion.lib.core.util.VersionUtil;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.web.item.HtmlItem;
import jp.ecuacion.splib.web.item.HtmlItemContainer;

/**
 * Is a record for configController.
 */
@ItemNameKeyClass("config")
public class ConfigRecord extends SplibRecord implements HtmlItemContainer {

  @Override
  public HtmlItem[] customizedItems() {
    return new HtmlItem[] {};
  }

  /**
   * get appVersion.
   * 
   * @return String
   */
  public String getAppVersion() {
    return getProductVersion("");
  }

  public String getCodeGeneratorExcelTemplateVersion() {
    return getProductVersion("ecuacion-tool-code-generator-excel-format");
  }

  public String getCodeGeneratorVersion() {
    return getProductVersion("ecuacion-tool-code-generator");
  }

  public String getSplibVersion() {
    return getProductVersion("ecuacion-splib");
  }

  public String getLibVersion() {
    return getProductVersion("ecuacion-lib");
  }

  public String getUtilsVersion() {
    return getProductVersion("ecuacion-utils");
  }

  private String getProductVersion(String productName) {
    String version = VersionUtil.getVersion(productName);
    return version == null ? "(none)" : version;
  }

}
