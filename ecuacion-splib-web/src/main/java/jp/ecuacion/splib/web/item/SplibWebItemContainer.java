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
import java.util.List;
import jp.ecuacion.lib.core.item.EclibItem;
import jp.ecuacion.lib.core.item.EclibItemContainer;
import jp.ecuacion.splib.web.bean.HtmlItem;
import jp.ecuacion.splib.web.bean.HtmlItemNumber;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;

/**
 * Has features related web environment.
 */
public interface SplibWebItemContainer extends EclibItemContainer {

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
  default HtmlItem[] mergeHtmlItems(HtmlItem[] fields1, HtmlItem[] fields2) {
    EclibItem[] items = mergeItems(fields1, fields2);
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
  * @param propertyPath propertyPath
  * @return boolean
  */
  default boolean needsCommas(String rootRecordName, String propertyPath) {
    HtmlItem item = getHtmlItem(propertyPath);

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
  default List<String> getNotEmptyFields(String loginState, RolesAndAuthoritiesBean bean) {
    return Arrays.asList(getHtmlItems()).stream()
        .filter(item -> item.getIsNotEmpty(loginState, bean))
        .map(item -> item.getItemPropertyPath()).toList();
  }

  /**
   * Returns whether isNotEmpty or not.
   * 
   * @param fieldId fieldId
   * @param loginState loginState
   * @param rolesOrAuthoritiesString rolesOrAuthoritiesString
   * @return boolean
   */
  default boolean isNotEmpty(String fieldId, String loginState, String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getNotEmptyFields(loginState, bean).contains(fieldId);
  }

  /**
   * Obtrains NotEmpty fields.
   * 
   * @param loginState loginState
   * @param bean bean
   * @return {@code List<String>}
   */
  default List<String> getNotEmptyOnSearchFields(String loginState, RolesAndAuthoritiesBean bean) {
    return Arrays.asList(getHtmlItems()).stream()
        .filter(item -> item.getIsNotEmptyOnSearch(loginState, bean))
        .map(item -> item.getItemPropertyPath()).toList();
  }

  /**
   * Returns whether isNotEmpty or not.
   * 
   * @param fieldId fieldId
   * @param loginState loginState
   * @param rolesOrAuthoritiesString rolesOrAuthoritiesString
   * @return boolean
   */
  default boolean isNotEmptyOnSearch(String fieldId, String loginState,
      String rolesOrAuthoritiesString) {
    SplibSecurityUtil.RolesAndAuthoritiesBean bean =
        new SplibSecurityUtil().getRolesAndAuthoritiesBean(rolesOrAuthoritiesString);
    return getNotEmptyOnSearchFields(loginState, bean).contains(fieldId);
  }
}
