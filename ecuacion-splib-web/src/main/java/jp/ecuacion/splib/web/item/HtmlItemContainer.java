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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jp.ecuacion.lib.core.item.EclibItem;
import jp.ecuacion.lib.core.item.EclibItemContainer;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.splib.web.record.StringMatchingConditionBean;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;

/**
 * Has features related web environment.
 */
public interface HtmlItemContainer extends EclibItemContainer {

  /**
   * Returns HtmlItem.
   * 
   * @return HtmlItem[]
   */
  public HtmlItem[] getHtmlItems();

  /**
   * Returns HtmlItem.
   * 
   * @return HtmlItem[]
   */
  default HtmlItem[] getItems() {
    return (HtmlItem[]) getHtmlItems();
  }

  /**
   * Merge htmlItems.
   */
  default HtmlItem[] mergeHtmlItems(HtmlItem[] items1, HtmlItem[] items2) {
    EclibItem[] items = mergeItems(items1, items2);
    List<HtmlItem> list = Arrays.asList(items).stream().map(item -> (HtmlItem) item).toList();
    return list.toArray(new HtmlItem[list.size()]);
  }

  /**
   * Returns {@code HtmlItem} from {@code HtmlItem[]} and {@code fieldId}. 
   * 
   * @param itemPropertyPath itemPropertyPath
   * @return HtmlItem
   */
  default HtmlItem getHtmlItem(String itemPropertyPath) {
    return (HtmlItem) getItem(itemPropertyPath);
  }

  /**
   * Returns a new instance.
   */
  public default HtmlItem getNewItem(String itemPropertyPath) {
    return new HtmlItem(itemPropertyPath);
  }

  /**
  * Returns whether the propertyPath needs comma.
  *
  * @param itemPropertyPath itemPropertyPath
  * @return boolean
  */
  default boolean needsCommas(String rootRecordName, String itemPropertyPath) {
    HtmlItem item = getHtmlItem(itemPropertyPath);

    if (item == null || !(item instanceof HtmlItemNumber)) {
      return false;
    }

    HtmlItemNumber numItem = (HtmlItemNumber) item;
    return numItem.getNeedsCommas();
  }

  /**
   * Obtrains NotEmpty fields.
   * 
   * @param loginState loginState
   * @param bean bean
   * @return {@code List<String>}
   */
  default List<String> getNotEmptyItemPropertyPathList(String loginState,
      RolesAndAuthoritiesBean bean) {
    return Arrays.asList(getHtmlItems()).stream()
        .filter(item -> item.getIsNotEmpty(loginState, bean))
        .map(item -> item.getItemPropertyPath()).toList();
  }

  /**
   * Returns whether isNotEmpty or not.
   * 
   * @param itemPropertyPath itemPropertyPath
   * @param loginState loginState
   * @param rolesOrAuthoritiesString rolesOrAuthoritiesString
   * @return boolean
   */
  default boolean isNotEmpty(String itemPropertyPath, String loginState,
      String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getNotEmptyItemPropertyPathList(loginState, bean).contains(itemPropertyPath);
  }

  /**
   * Obtrains NotEmpty fields.
   * 
   * @param loginState loginState
   * @param bean bean
   * @return {@code List<String>}
   */
  default List<String> getNotEmptyOnSearchItemPropertyPathList(String loginState,
      RolesAndAuthoritiesBean bean) {
    return Arrays.asList(getHtmlItems()).stream()
        .filter(item -> item.getIsNotEmptyOnSearch(loginState, bean))
        .map(item -> item.getItemPropertyPath()).toList();
  }

  /**
   * Returns whether isNotEmpty or not.
   * 
   * @param itemPropertyPath itemPropertyPath
   * @param loginState loginState
   * @param rolesOrAuthoritiesString rolesOrAuthoritiesString
   * @return boolean
   */
  default boolean isNotEmptyOnSearch(String itemPropertyPath, String loginState,
      String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getNotEmptyOnSearchItemPropertyPathList(loginState, bean).contains(itemPropertyPath);
  }

  /** Returns search pattern for each item. */
  default Map<String, StringMatchingConditionBean> getSearchPatterns() {
    Map<String, StringMatchingConditionBean> map = new HashMap<>();

    HtmlItem[] htmlItems = getHtmlItems();
    for (HtmlItem item : htmlItems) {
      if (item instanceof HtmlItemString
          && ((HtmlItemString) item).getStringSearchPatternEnum() != null) {
        HtmlItemString itemStr = (HtmlItemString) item;
        map.put(item.getItemPropertyPath(), new StringMatchingConditionBean(
            itemStr.getStringSearchPatternEnum(), itemStr.isIgnoresCase()));
      }
    }

    return map;
  }

  /**
   * Gets SearchPatternComment.
   * 
   * @param locale locale
   * @param itemPropertyPath itemPropertyPath
   * @return String
   */
  default String getSearchPatternComment(Locale locale, String itemPropertyPath) {
    StringMatchingConditionBean bean = getSearchPatterns().get(itemPropertyPath);
    if (bean == null) {
      throw new RuntimeException("The stringMatchingCondition of the field '" + itemPropertyPath
          + "' not set'. At least you need to add '" + itemPropertyPath
          + "' to the getHtmlItems() in 'xxxSearchRecord or it's parent Record.");
    }

    String commentMessageId =
        StringUtil.getLowerCamelFromSnake(bean.getStringSearchPatternEnum().toString());

    return PropertyFileUtil.getMessage(locale,
        "jp.ecuacion.splib.web.common.label.searchPattern." + commentMessageId + "Match");
  }
}
