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
package jp.ecuacion.splib.web.form.record;

import java.util.ResourceBundle;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.splib.core.form.record.SplibRecord;
import jp.ecuacion.splib.web.bean.HtmlField;

/**
 * Is a record for configController.
 */
public class ConfigRecord extends SplibRecord implements RecordInterface {
  private static final String codeGeneratorNotUsedMesssage = "(code-generator not used)";

  @Override
  public HtmlField[] getHtmlFields() {
    return new HtmlField[] {};
  }

  public String getAppVersion() {
    return ResourceBundle.getBundle("version").getString("project.version");
  }

  public String getExcelTemplateVersion() {
    return getCodeGeneratorRelatedValue("EXCEL_TEMPLATE_VERSION");
  }

  public String getCodeGeneratorVersion() {
    return getCodeGeneratorRelatedValue("CODE_GENERATOR_VERSION");
  }

  private String getCodeGeneratorRelatedValue(String key) {
    boolean hasValue = PropertyFileUtil.hasApp(key);
    return hasValue ? PropertyFileUtil.getApp(key) : codeGeneratorNotUsedMesssage;
  }

}
