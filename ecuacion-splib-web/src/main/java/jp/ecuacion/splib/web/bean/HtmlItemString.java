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
package jp.ecuacion.splib.web.bean;

import jp.ecuacion.splib.web.form.record.StringMatchingConditionBean;
import jp.ecuacion.splib.web.form.record.StringMatchingConditionBean.StringMatchingPatternEnum;

/**
 * Is used for string field.
 */
public class HtmlItemString extends HtmlItem {

  protected StringMatchingConditionBean matchingCondition;

  /**
   * Constructs a new instance.
   * 
   * @see HtmlItem
   * @param itemPropertyPath propertyPath
   */
  public HtmlItemString(String itemPropertyPath) {
    super(itemPropertyPath);
    this.matchingCondition = new StringMatchingConditionBean();
  }

  @Override
  public HtmlItemString itemNameKey(String itemNameKey) {
    return (HtmlItemString) super.itemNameKey(itemNameKey);
  }

  @Override
  public HtmlItemString isNotEmpty(boolean isNotEmpty) {
    return (HtmlItemString) super.isNotEmpty(isNotEmpty);
  }

  @Override
  public HtmlItemString isNotEmpty(HtmlItemConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    return (HtmlItemString) super.isNotEmpty(authKind, authString, isNotEmpty);
  }

  /**
   * Offers the string search pattern.
   * 
   * @param stringMatchingPattern stringMatchingPattern
   * @param ignoresCase ignoresCase
   * @return HtmlItemString
   */
  public HtmlItemString stringMatchingCondition(StringMatchingPatternEnum stringMatchingPattern,
      boolean ignoresCase) {
    this.matchingCondition = new StringMatchingConditionBean(stringMatchingPattern, ignoresCase);
    return this;
  }

  public StringMatchingPatternEnum getStringSearchPatternEnum() {
    return matchingCondition.getStringSearchPatternEnum();
  }

  public boolean isIgnoresCase() {
    return matchingCondition.isIgnoresCase();
  }
}
