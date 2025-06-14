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
package jp.ecuacion.splib.web.util;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import jp.ecuacion.splib.web.bean.HtmlItem;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.form.record.RecordInterface;
import org.springframework.stereotype.Component;

/**
 * Provides utility methods on records and fields.
 */
@Component("recUtil")
public class SplibRecordUtil {

  /**
   * Returns {@code HtmlItem} 
   *     from {@code SplibGeneralForm}, {@code rootRecordId} and {@code fieldId}. 
   * 
   * @param form form
   * @param rootRecordName rootRecordName
   * @param itemPropertyPath propertyPath
   * @return HtmlItem
   */
  public HtmlItem getHtmlItem(SplibGeneralForm form, String rootRecordName,
      String itemPropertyPath) {

    return getHtmlItem(((RecordInterface) form.getRootRecord(rootRecordName)).getHtmlItems(),
        rootRecordName, itemPropertyPath);
  }

  /**
   * Returns {@code HtmlItem} 
   *     from {@code SplibGeneralForm}, {@code rootRecordId} and {@code fieldId}. 
   * 
   * <p>Actually more accurate code is needed in the future 
   *     but for now just 0th form of the forms is used.</p> 
   * 
   * @param forms forms
   * @param rootRecordId rootRecordId
   * @param itemKindId itemKindId
   * @return HtmlItem
   */
  public HtmlItem getHtmlItem(SplibGeneralForm[] forms, String rootRecordId, String itemKindId) {
    return getHtmlItem(forms[0], rootRecordId, itemKindId);
  }

  /**
   * Returns {@code HtmlItem} from {@code HtmlItem[]} and {@code fieldId}. 
   * 
   * @param htmlItems htmlItems
   * @param itemPropertyPath itemPropertyPath
   * @return HtmlItem
   */
  public HtmlItem getHtmlItem(HtmlItem[] htmlItems, String rootRecordName,
      String itemPropertyPath) {
    Map<String, HtmlItem> map = Arrays.asList(htmlItems).stream()
        .collect(Collectors.toMap(e -> e.getItemKindIdField(), e -> e));

    HtmlItem field = map.get(itemPropertyPath);

    // Try with itemPropertyPathWithoutRootRecord
    if (field == null && itemPropertyPath.startsWith(rootRecordName)) {
      field = map.get(itemPropertyPath.substring(rootRecordName.length() + 1));
    }

    return field == null ? new HtmlItem(itemPropertyPath) : field;
  }
}
