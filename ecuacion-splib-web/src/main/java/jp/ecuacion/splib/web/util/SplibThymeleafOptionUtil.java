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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import org.springframework.stereotype.Component;

/**
 * thymeleaf側から呼ばれる想定のクラス。 html componentsで使用されるoptionsを解析する。
 * optionsはcsv形式で、複数のoptional属性を受け入れる。'readonly,required'など。
 * また、値付きのパラメータも受け入れ可能とするため、a=bの形（'readonly,a=b,required'など）の形も可とする。
 * readonlyなどのパラメータがないものはMapのkeyにのみ設定（valueはnull）とし、bのような値はvalueに入れることとする。
 */
@Component("optUtil")
public class SplibThymeleafOptionUtil {

  private HttpServletRequest request;

  public SplibThymeleafOptionUtil(HttpServletRequest request) {
    this.request = request;
  }

  /**
   * 削除ボタン押下時のメッセージ文字列を生成。
   * itemDisplayedOnDeleteには、通常は一つのitemを指定するが、"itemA,itemB"のように複数の項目を指定することも可能。 （natural
   * keyが複数の項目からなる場合に使用） その場合は、(itemA, itemB) : (1, 2) のように括弧で括った形で表現
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

      // 指定されたfieldが実はSystemCommonEntityに定義されている場合を救っておく。
      if (!PropertyFileUtil.hasItemName(item)) {
        String field = item.contains(".") ? item.substring(item.lastIndexOf(".") + 1) : item;
        String commonItem = "SystemCommonEntity." + field;
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

    return PropertyFileUtil.getMsg(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.deleteConfirmation",
        new String[] {fieldNames.toString()});
  }

  private Map<String, String> optionMap(String optionCsv) {

    Map<String, String> rtnMap = new HashMap<>();

    // optionCsvが空の場合は終了
    if (optionCsv == null || optionCsv.equals("")) {
      return rtnMap;
    }

    String key = null;
    String value = null;
    String[] options = optionCsv.split(",");
    for (String option : options) {
      if (option.contains("=")) {
        key = option.substring(0, option.indexOf("="));
        value = option.substring(option.indexOf("=") + 1);

      } else {
        key = option;
      }

      // 多少の間違いを拾うため、全てのkeyの文字をlowerCaseにしておく
      String lowerCaseKey = key.toLowerCase();
      rtnMap.put(lowerCaseKey, value);
    }

    return rtnMap;
  }

  /** keyの存在チェック。keyがあれば処理するパターンのoptionはこれのみの使用で処理分岐可能。 */
  public boolean hasKey(String options, String key) {
    return optionMap(options).containsKey(key.toLowerCase());
  }

  private String getValue(String options, String key) {
    return optionMap(options).get(key.toLowerCase());
  }

  private String defaultIfKeyNotExist(String options, String key, String defaultValue) {
    if (hasKey(options, key)) {
      return getValue(options, key);

    } else {
      return Integer.valueOf(defaultValue).toString();
    }
  }

  @Deprecated
  public boolean isReadOnly(String options) {
    return hasKey(options, "readonly");
  }

  @Deprecated
  public boolean isDisabled(String options) {
    return hasKey(options, "disabled");
  }

  @Deprecated
  public boolean isDeleted(String options) {
    return hasKey(options, "deleted");
  }

  @Deprecated
  public boolean isForSwitch(String options) {
    return hasKey(options, "forSwitch");
  }

  @Deprecated
  public boolean needsEmptyOption(String options) {
    return !hasKey(options, "noEmptyOption");
  }

  @Deprecated
  public boolean needsOnclickJs(String options) {
    return hasKey(options, "onClickJs");
  }

  @Deprecated
  public boolean hasLinkUrl(String options) {
    return hasKey(options, "linkUrl");
  }

  public String getLinkUrl(String options) {
    return getValue(options, "linkUrl");
  }

  public String rows(String options) {
    return defaultIfKeyNotExist(options, "rows", "1");
  }

  public String cols(String options) {
    return defaultIfKeyNotExist(options, "cols", "1");
  }

  @Deprecated
  public boolean hasThSortable(String options) {
    return hasKey(options, "thSortable");
  }

  public boolean isWidthFull(String options) {
    return widthCheck(options, "full");
  }

  public boolean isWidthHalf(String options) {
    return widthCheck(options, "half");
  }

  private boolean widthCheck(String options, String value) {
    String key = "width";
    if (!hasKey(options, key)) {
      return false;
    }

    return getValue(options, key).equalsIgnoreCase(value) ? true : false;
  }
}
