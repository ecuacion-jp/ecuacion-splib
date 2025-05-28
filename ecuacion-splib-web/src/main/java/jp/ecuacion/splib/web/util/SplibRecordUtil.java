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
import java.util.stream.Collectors;
import jp.ecuacion.splib.web.bean.HtmlItem;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.form.record.RecordInterface;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

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
   * @param rootRecordId rootRecordId
   * @param itemId itemId
   * @return HtmlItem
   */
  public HtmlItem getHtmlItem(SplibGeneralForm form, String rootRecordId, String itemId) {
    if (!StringUtils.capitalize(itemId).startsWith(StringUtils.capitalize(rootRecordId) + ".")) {
      throw new RuntimeException("itemId: " + itemId + " does not start with rootRecordId: "
          + rootRecordId + " plus dot(.).");
    }

    String fieldId = itemId.substring((rootRecordId + ".").length());

    return getHtmlItem(((RecordInterface) form.getRootRecord(rootRecordId)).getHtmlItems(),
        fieldId);
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
   * @param itemId itemId
   * @return HtmlItem
   */
  public HtmlItem getHtmlItem(SplibGeneralForm[] forms, String rootRecordId, String itemId) {
    return getHtmlItem(forms[0], rootRecordId, itemId);
  }

  /**
   * Returns {@code HtmlItem} from {@code HtmlItem[]} and {@code fieldId}. 
   * 
   * @param htmlItems htmlItems
   * @param fieldId fieldId
   * @return HtmlItem
   */
  public HtmlItem getHtmlItem(HtmlItem[] htmlItems, String fieldId) {
    HtmlItem field = Arrays.asList(htmlItems).stream()
        .collect(Collectors.toMap(e -> e.getItemIdField(), e -> e)).get(fieldId);

    return field == null ? new HtmlItem(fieldId) : field;
  }
}
