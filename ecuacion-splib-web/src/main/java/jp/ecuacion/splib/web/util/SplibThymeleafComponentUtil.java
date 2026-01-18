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

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import org.springframework.stereotype.Component;

/**
 * Offers utility methods for components.
 */
@Component("compUtil")
public class SplibThymeleafComponentUtil {

  private HttpServletRequest request;

  /**
   * Constructs a new instance.
   * 
   * @param request request
   */
  public SplibThymeleafComponentUtil(HttpServletRequest request) {
    this.request = request;
  }

  /**
   * Provides message string shown when the delete button pressed.
   * 
   * <p>itemDisplayedOnDeleteには、通常は一つのitemを指定するが、"itemA,itemB"のように複数の項目を指定することも可能。 （natural
   * keyが複数の項目からなる場合に使用） その場合は、(itemA, itemB) : (1, 2) のように括弧で括った形で表現</p>
   */
  public String getDeleteConfirmMessage(String rootRecordName, String itemDisplayedOnDelete) {
    List<String> fieldNameList = new ArrayList<>();

    for (String item : itemDisplayedOnDelete.split(",")) {
      // itemDisplayedOnDeleteに"."が2つ以上ある場合はメッセージ表示に適さないのでパスを短くする
      while (true) {
        if (item.split("\\.").length <= 2) {
          break;
        }

        item = item.substring(item.indexOf(".") + 1);
      }

      // 指定されたfieldが実はSystemCommonに定義されている場合を救っておく。
      if (!PropertyFileUtil.hasItemName(item)) {
        String field = item.contains(".") ? item.substring(item.lastIndexOf(".") + 1) : item;
        String commonItem = "SystemCommon." + field;
        if (PropertyFileUtil.hasItemName(commonItem)) {
          item = commonItem;
        }
      }

      fieldNameList
          .add(PropertyFileUtil.getItemName(request.getLocale(), rootRecordName + "." + item));
    }

    boolean multiple = fieldNameList.size() > 1;

    StringBuilder fieldNameSb = new StringBuilder();
    for (String str : fieldNameList) {
      fieldNameSb.append(", " + str);
    }

    String fieldNames =
        (multiple ? "（" : "") + fieldNameSb.toString().substring(2) + (multiple ? "）" : "");

    return PropertyFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.deleteConfirmation",
        new String[] {fieldNames.toString()});
  }
}
