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
import java.util.stream.Stream;
import jp.ecuacion.lib.core.item.Item;
import jp.ecuacion.lib.core.item.ItemContainer;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.util.PropertyPathUtil;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.splib.web.bean.StringMatchingConditionBean;
import jp.ecuacion.splib.web.util.SplibSecurityUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Has features related web environment.
 */
public interface HtmlItemContainer extends ItemContainer {

  /**
   * Returns {@code HtmlItem[]} for this class level.
   *
   * <p>Override this method instead of {@link #customizedItems()} when implementing
   *     {@code HtmlItemContainer}. The framework collects items from the entire class hierarchy
   *     automatically.</p>
   *
   * @return HtmlItem[]
   */
  @Override
  HtmlItem[] customizedItems();

  /**
   * Returns all {@code HtmlItem}s merged across the entire class hierarchy.
   *
   * <p>Each item is resolved via {@link #getHtmlItem(String)}, which applies the same
   *     property-level inheritance as {@link ItemContainer#getItem(String)}.</p>
   *
   * @return HtmlItem[]
   */
  default HtmlItem[] getHtmlItems() {
    Logger log = LoggerFactory.getLogger(HtmlItemContainer.class);
    return allCustomizedPropertyPaths().stream()
        .flatMap(path -> {
          try {
            return Stream.of(getHtmlItem(path));
          } catch (RuntimeException e) {
            Throwable cause = e;
            while (cause != null) {
              if (cause instanceof NoSuchFieldException nsfe) {
                log.info("HtmlItem '{}' not found, skipping. detail: {}", path, nsfe.getMessage());
                return Stream.empty();
              }
              cause = cause.getCause();
            }
            throw e;
          }
        })
        .toArray(HtmlItem[]::new);
  }

  /**
   * Returns the value of the field specified by {@code propertyPath}.
   *
   * <p>Uses {@link PropertyPathUtil#getValue(Object, String)} internally.</p>
   *
   * @param propertyPath property path (e.g. {@code "name"})
   * @return the field value, or {@code null} if not found
   */
  default @Nullable Object getValue(String propertyPath) {
    return PropertyPathUtil.getValue(this, propertyPath);
  }

  /**
   * Merge htmlItems.
   */
  default HtmlItem[] mergeHtmlItems(HtmlItem[] items1, HtmlItem[] items2) {
    Item[] items = mergeItems(items1, items2);
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
  @Override
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

    if (item == null || !(item instanceof HtmlItemNumber numItem)) {
      return false;
    }

    return numItem.getNeedsCommas();
  }

  /**
   * Obtrains NotEmpty fields.
   *
   * @param loginState loginState
   * @param bean bean
   * @return {@code List<String>}
   */
  @SuppressWarnings("null")
  default List<String> getNotEmptyItemPropertyPathList(String loginState,
      RolesAndAuthoritiesBean bean) {
    return Arrays.asList(getHtmlItems()).stream()
        .filter(item -> item.getIsNotEmpty(loginState, bean))
        .map(item -> item.getPropertyPath()).toList();
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

  /** Returns search pattern for each item. */
  default Map<String, StringMatchingConditionBean> getSearchPatterns() {
    Map<String, StringMatchingConditionBean> map = new HashMap<>();

    HtmlItem[] htmlItems = getHtmlItems();
    for (HtmlItem item : htmlItems) {
      if (item instanceof HtmlItemString itemStr
          && itemStr.getStringSearchPatternEnum() != null) {
        map.put(item.getPropertyPath(), new StringMatchingConditionBean(
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

    return PropertiesFileUtil.getMessage(locale,
        "jp.ecuacion.splib.web.common.label.searchPattern." + commentMessageId + "Match");
  }
}
