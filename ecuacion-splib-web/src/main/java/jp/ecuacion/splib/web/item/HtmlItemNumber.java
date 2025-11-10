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
package jp.ecuacion.splib.web.item;

/**
 * Is used for number item.
 */
public class HtmlItemNumber extends HtmlItem {

  /**
   * Determines whether commas are used or not.
   */
  protected boolean needsCommas;

  /**
   * Constructs a new instance.
   * 
   * @see HtmlItem
   * @param itemPropertyPath propertyPath
   */
  public HtmlItemNumber(String itemPropertyPath) {
    super(itemPropertyPath);
  }

  @Override
  public HtmlItemNumber itemNameKey(String itemNameKey) {
    return (HtmlItemNumber) super.itemNameKey(itemNameKey);
  }
  
  @Override
  public HtmlItemNumber hideValue() {
    return (HtmlItemNumber) super.hideValue();
  }

  @Override
  public HtmlItemNumber notEmpty() {
    return (HtmlItemNumber) super.notEmpty();
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

  @Override
  public HtmlItemNumber notEmptyOnSearch() {
    return (HtmlItemNumber) super.notEmptyOnSearch();
  }
  
  @Override
  public HtmlItemNumber isNotEmptyOnSearch(boolean isNotEmpty) {
    return (HtmlItemNumber) super.isNotEmptyOnSearch(isNotEmpty);
  }

  @Override
  public HtmlItemNumber isNotEmptyOnSearch(HtmlItemConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    return (HtmlItemNumber) super.isNotEmptyOnSearch(authKind, authString, isNotEmpty);
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
