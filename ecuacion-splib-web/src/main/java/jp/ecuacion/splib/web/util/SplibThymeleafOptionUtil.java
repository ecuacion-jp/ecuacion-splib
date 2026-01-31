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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Analyzes options specified with html components.
 * 
 * <p>It's called from thymeleaf.<br>
 *     Options are in the format of csv and able to receive multiple optional attributes.
 *     It's written like 'readonly,required'.<br>
 *     It also receives key value parameter like 'readonly,id=deptName,required'.</p>
 */
@Component("optUtil")
public class SplibThymeleafOptionUtil {

  private DetailLogger detailLog = new DetailLogger(this);

  /**
   * Constructs a new instance.
   */
  public SplibThymeleafOptionUtil() {}

  /**
   * Creates Map from option csv.
   * 
   * <p>When duplicated keys exist, the result differs by the value of {@code allowsDuplicate}.</p>
   * 
   * <ul>
   * <li>When allowsDuplicateKey == false : maximum number of elements in the value list is one.<br>
   *     when a key is duplicated (like key1 in 'key1=a,key2,key1=b'), 
   *     former one is overrided and ignored and the latter one adopted 
   *     (in the example above, key1=a is ignored and key1=b is adopted).</li>
   * <li>When allowsDuplicateKey == true  : all the values are put into the value array.<br>
   *     If option is like 'key1=a,key2,key1=b,key1=a',
   *     then the value for key1 is {@code {'a', 'b', 'a')}}</li>
   * </ul>
   */
  private Map<String, List<String>> optionMap(String optionCsv, boolean allowsDuplicateKey) {

    Map<String, List<String>> rtnMap = new HashMap<>();

    // Finish when optionCsv is empty.
    if (optionCsv == null || optionCsv.equals("")) {
      return rtnMap;
    }

    String key = null;
    String value = null;
    String[] options = optionCsv.split(",");
    for (String option : options) {
      if (option.contains("=")) {
        key = StringUtils.trim(option.substring(0, option.indexOf("=")));
        value = StringUtils.trim(option.substring(option.indexOf("=") + 1));

      } else {
        key = option;
      }

      // Ignore empty key because it happens and it's not bad.
      // (It happens when you set an option conditionally like:
      // options = 'a=b,' + (c == null ? '' : 'c=d')
      if (key.equals("")) {
        continue;
      }

      // Change key string to lowercase to ignore case mistakes.
      String lowerCaseKey = key.toLowerCase();

      if (!rtnMap.containsKey(lowerCaseKey)) {
        rtnMap.put(lowerCaseKey, new ArrayList<>());
      }

      // if the key already exists in the map and allowsDuplicateKey == false,
      // the value is updated and output log.
      if (rtnMap.get(lowerCaseKey).size() != 0 && !allowsDuplicateKey) {
        rtnMap.get(lowerCaseKey).clear();
        detailLog.warn("html key is dupliicated in options. Duplicated key: " + key);
      }

      rtnMap.get(lowerCaseKey).add(value);
    }

    return rtnMap;
  }

  /**
   * Returns if specified key exists in options.
   */
  public boolean hasKey(String options, String key) {
    return optionMap(options, false).containsKey(key.toLowerCase());
  }

  /**
   * Returns value obtained from the key.
   * 
   * @param options options
   * @param key key
   * @return value
   */
  public String getValue(String options, String key) {
    String lowerCaseKey = key.toLowerCase();
    Map<String, List<String>> map = (optionMap(options, false));

    return map.containsKey(lowerCaseKey) ? (map.get(lowerCaseKey)).get(0) : null;
  }

  /**
   * Returns value obtained from the key, or defaultValue 
   * when the key does not exist in the properties file.
   * 
   * @param options options
   * @param key key
   * @param defaultValue defaultValue
   * @return value
   */
  public String getValueOrElse(String options, String key, String defaultValue) {
    if (hasKey(options, key)) {
      return getValue(options, key);

    } else {
      return defaultValue;
    }
  }

  /**
   * Returns values obtained from the key as an array.
   * 
   * @param options options
   * @param key key
   * @return value
   */
  public String[] getValues(String options, String key) {
    String lowerCaseKey = key.toLowerCase();
    Map<String, List<String>> map = (optionMap(options, true));

    if (map.containsKey(lowerCaseKey)) {
      List<String> list = (map.get(lowerCaseKey));
      return list.toArray(new String[list.size()]);

    } else {
      return null;
    }
  }
}
