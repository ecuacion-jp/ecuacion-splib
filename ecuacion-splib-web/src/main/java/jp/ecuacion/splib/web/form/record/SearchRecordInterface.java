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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.lib.core.util.StringUtil;
import jp.ecuacion.splib.web.bean.HtmlField;
import jp.ecuacion.splib.web.bean.HtmlFieldString;

public interface SearchRecordInterface {

  HtmlField[] getHtmlFields();

  /** 項目ごとのsearch patternを返す。全項目を設定する必要はなく、検索で使用する項目のみで良い。 */
  default Map<String, StringMatchingConditionBean> getSearchPatterns() {
    Map<String, StringMatchingConditionBean> map = new HashMap<>();

    HtmlField[] htmlItems = getHtmlFields();
    for (HtmlField item : htmlItems) {
      if (item instanceof HtmlFieldString
          && ((HtmlFieldString) item).getStringSearchPatternEnum() != null) {
        HtmlFieldString itemStr = (HtmlFieldString) item;
        map.put(item.getId(), new StringMatchingConditionBean(
            itemStr.getStringSearchPatternEnum(), itemStr.isIgnoresCase()));
      }
    }

    return map;
  }

  default String getSearchPatternComment(Locale locale, String fieldName) {
    StringMatchingConditionBean bean = getSearchPatterns().get(fieldName);
    if (bean == null) {
      throw new RuntimeException("The stringMatchingCondition of the field '" + fieldName
          + "' not set'. At least you need to add '" + fieldName
          + "' to the getHtmlItems() in 'xxxSearchRecord or it's parent Record.");
    }

    String commentMessageId =
        new StringUtil().getLowerCamelFromSnakeOrNullIfInputIsNull(bean.getStringSearchPatternEnum().toString());

    return PropertyFileUtil.getMsg(locale,
        "jp.ecuacion.splib.web.common.label.searchPattern." + commentMessageId + "Match");
  }
}
