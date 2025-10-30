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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.splib.web.bean.HtmlItem;
import jp.ecuacion.splib.web.bean.HtmlItemString;
import jp.ecuacion.splib.web.record.StringMatchingConditionBean;

/**
 * Has features related web environment and search function.
 */
public interface SplibWebSearchItemContainer extends SplibWebItemContainer {

  /** 項目ごとのsearch patternを返す。全項目を設定する必要はなく、検索で使用する項目のみで良い. */
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
   * @param fieldName fieldName
   * @return String
   */
  default String getSearchPatternComment(Locale locale, String fieldName) {
    StringMatchingConditionBean bean = getSearchPatterns().get(fieldName);
    if (bean == null) {
      throw new RuntimeException("The stringMatchingCondition of the field '" + fieldName
          + "' not set'. At least you need to add '" + fieldName
          + "' to the getHtmlItems() in 'xxxSearchRecord or it's parent Record.");
    }

    String commentMessageId =
        StringUtil.getLowerCamelFromSnake(bean.getStringSearchPatternEnum().toString());

    return PropertyFileUtil.getMessage(locale,
        "jp.ecuacion.splib.web.common.label.searchPattern." + commentMessageId + "Match");
  }
}
