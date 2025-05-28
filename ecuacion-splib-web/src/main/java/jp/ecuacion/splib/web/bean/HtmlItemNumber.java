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

/**
 * Is used for number field.
 */
public class HtmlItemNumber extends HtmlItem {

  /**
   * Determins whether commas are used or not.
   */
  protected boolean needsCommas;

  /**
   * Constructs a new instance.
   * 
   * @param id id
   */
  public HtmlItemNumber(String id) {
    super(id);
  }

  @Override
  public HtmlItemNumber itemIdFieldForName(String itemIdFieldForName) {
    return (HtmlItemNumber) super.itemIdFieldForName(itemIdFieldForName);
  }

  @Override
  public HtmlItemNumber isNotEmpty(boolean isNotEmpty) {
    return (HtmlItemNumber) super.isNotEmpty(isNotEmpty);
  }

  @Override
  public HtmlItemNumber isNotEmpty(HtmlItemConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    return (HtmlItemNumber) super.isNotEmpty(authKind, authString, isNotEmpty);
  }

  /**
   * Returns needsCommas.
   * 
   * @param needsCommas needsCommas
   * @return HtmlItemNumber
   */
  public HtmlItemNumber needsCommas(boolean needsCommas) {
    this.needsCommas = needsCommas;
    return this;
  }

  public boolean getNeedsCommas() {
    return needsCommas;
  }
}
