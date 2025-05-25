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
 * Analyzes options specified with html components.
 * 
 * <p>It's called from thymeleaf.<br>
 *     Options are in the format of csv and receives multiple optional attributes.
 *     It's written like 'readonly,required'.<br>
 *     It also receives key value parameter like 'readonly,a=b,required'.</p>
 */
@Component("optUtil")
public class SplibThymeleafOptionUtil {

  private HttpServletRequest request;

  /**
   * Constructs a new instance.
   * 
   * @param request request
   */
  public SplibThymeleafOptionUtil(HttpServletRequest request) {
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

    return PropertyFileUtil.getMessage(request.getLocale(),
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

  /**
   * Returns if specified key exists in options.
   */
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

  /**
   * Returns whether the options contain readonly.
   * 
   * @param options options
   * @return boolean
   */
  public boolean isReadOnly(String options) {
    return hasKey(options, "readonly");
  }

  /**
   * Returns whether the options contain disabled.
   * 
   * @param options options
   * @return boolean
   */
  public boolean isDisabled(String options) {
    return hasKey(options, "disabled");
  }

  /**
   * Returns whether the options contain deleted.
   * 
   * @param options options
   * @return boolean
   */
  public boolean isDeleted(String options) {
    return hasKey(options, "deleted");
  }

  /**
   * Returns whether the options contain forSwitch.
   * 
   * @param options options
   * @return boolean
   */
  public boolean isForSwitch(String options) {
    return hasKey(options, "forSwitch");
  }

  /**
   * Returns whether the options contain noEmptyOption.
   * 
   * @param options options
   * @return boolean
   */
  public boolean needsEmptyOption(String options) {
    return !hasKey(options, "noEmptyOption");
  }

  /**
   * Returns whether the options contain onClickJs.
   * 
   * @param options options
   * @return boolean
   */
  public boolean needsOnclickJs(String options) {
    return hasKey(options, "onClickJs");
  }

  /**
   * Returns whether the options contain linkUrl.
   * 
   * @param options options
   * @return boolean
   */
  public boolean hasLinkUrl(String options) {
    return hasKey(options, "linkUrl");
  }

  /**
   * Returns linkUrl.
   * 
   * @param options options
   * @return linkUrl
   */
  public String getLinkUrl(String options) {
    return getValue(options, "linkUrl");
  }


  /**
   * Returns rows with default value 1.
   * 
   * @param options options
   * @return linkUrl
   */
  public String rows(String options) {
    return defaultIfKeyNotExist(options, "rows", "1");
  }

  /**
   * Returns cols with default value 1.
   * 
   * @param options options
   * @return linkUrl
   */
  public String cols(String options) {
    return defaultIfKeyNotExist(options, "cols", "1");
  }

  /**
   * Returns whether the options contain thSortable.
   * 
   * @param options options
   * @return boolean
   */
  public boolean hasThSortable(String options) {
    return hasKey(options, "thSortable");
  }

  /**
   * Returns whether the options contain full.
   * 
   * @param options options
   * @return boolean
   */
  public boolean isWidthFull(String options) {
    return widthCheck(options, "full");
  }

  /**
   * Returns whether the options contain half.
   * 
   * @param options options
   * @return boolean
   */
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
