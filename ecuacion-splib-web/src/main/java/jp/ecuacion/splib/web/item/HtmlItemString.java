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

import jp.ecuacion.splib.web.bean.StringMatchingConditionBean;
import jp.ecuacion.splib.web.bean.StringMatchingConditionBean.StringMatchingPatternEnum;

/**
 * Is used for string item.
 */
public class HtmlItemString extends HtmlItem {

  protected StringMatchingConditionBean matchingCondition;

  /** See {@link #excludeFromExplicitSearchConditions()}. */
  private boolean excludedFromExplicitSearchConditions = false;

  /**
   * Constructs a new instance.
   * 
   * @see HtmlItem
   * @param itemPropertyPath itemPropertyPath
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
  public HtmlItemString hideValue() {
    return (HtmlItemString) super.hideValue();
  }

  @Override
  public HtmlItemString notEmpty() {
    return (HtmlItemString) super.notEmpty();
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
   * <p>The pattern is used for two purposes:</p>
   * <ul>
   *   <li>The search pattern comment (like "partial match") shown next to the input field
   *       by {@code bootstrap/components-search :: searchText}.
   *       ({@code searchText} requires this setting, otherwise an exception is thrown.)</li>
   *   <li>The search condition automatically created
   *       by {@code SpecFactory#addExplicitSearchConditions(SplibRecord)} in splib-web-jpa,
   *       which searches the entity field with the same itemPropertyPath.</li>
   * </ul>
   *
   * <p>If the item is not an entity field (for example, a search item which matches
   *     one of multiple fields), call {@link #excludeFromExplicitSearchConditions()} too
   *     so that only the comment is shown and you can create the condition by yourself.</p>
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

  /**
   * Excludes the item from the search conditions automatically created
   *     by {@code SpecFactory#addExplicitSearchConditions(SplibRecord)} in splib-web-jpa.
   *
   * <p>The search pattern comment set by
   *     {@link #stringMatchingCondition(StringMatchingPatternEnum, boolean)} is still shown
   *     by {@code bootstrap/components-search :: searchText}.
   *     Use this when the item is not an entity field and you create the condition by yourself.
   *     </p>
   *
   * @return HtmlItemString
   */
  public HtmlItemString excludeFromExplicitSearchConditions() {
    this.excludedFromExplicitSearchConditions = true;
    return this;
  }

  /**
   * Returns whether the item is excluded from the search conditions automatically created
   *     by {@code SpecFactory#addExplicitSearchConditions(SplibRecord)}.
   *
   * @return boolean
   */
  public boolean isExcludedFromExplicitSearchConditions() {
    return excludedFromExplicitSearchConditions;
  }

  public StringMatchingPatternEnum getStringSearchPatternEnum() {
    return matchingCondition.getStringSearchPatternEnum();
  }

  public boolean isIgnoresCase() {
    return matchingCondition.isIgnoresCase();
  }
}
