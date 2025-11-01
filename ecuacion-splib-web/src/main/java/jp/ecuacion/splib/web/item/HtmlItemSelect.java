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
public class HtmlItemSelect extends HtmlItem {

  /**
   * Constructs a new instance.
   * 
   * @see HtmlItem
   * @param itemPropertyPath propertyPath
   */
  public HtmlItemSelect(String itemPropertyPath) {
    super(itemPropertyPath);
  }

  @Override
  public HtmlItemSelect itemNameKey(String itemNameKey) {
    return (HtmlItemSelect) super.itemNameKey(itemNameKey);
  }

  @Override
  public HtmlItemSelect notEmpty() {
    return (HtmlItemSelect) super.notEmpty();
  }
  
  @Override
  public HtmlItemSelect isNotEmpty(boolean isNotEmpty) {
    return (HtmlItemSelect) super.isNotEmpty(isNotEmpty);
  }

  @Override
  public HtmlItemSelect isNotEmpty(HtmlItemConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    return (HtmlItemSelect) super.isNotEmpty(authKind, authString, isNotEmpty);
  }

  @Override
  public HtmlItemSelect notEmptyOnSearch() {
    return (HtmlItemSelect) super.notEmptyOnSearch();
  }
  
  @Override
  public HtmlItemSelect isNotEmptyOnSearch(boolean isNotEmpty) {
    return (HtmlItemSelect) super.isNotEmptyOnSearch(isNotEmpty);
  }

  @Override
  public HtmlItemSelect isNotEmptyOnSearch(HtmlItemConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    return (HtmlItemSelect) super.isNotEmptyOnSearch(authKind, authString, isNotEmpty);
  }
}
