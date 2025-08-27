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

import java.util.HashMap;
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
  public SplibThymeleafOptionUtil() {
  }

  /**
   * Creates Map from option csv.
   * 
   * <p>When duplicated keys exist, former one is overrided and ignored.</p>
   * 
   * @param optionCsv optionCsv
   * @return Map
   */
  private Map<String, String> optionMap(String optionCsv) {

    Map<String, String> rtnMap = new HashMap<>();

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

      // Change key string to lowercase to ignore case mistakes.
      String lowerCaseKey = key.toLowerCase();

      // if the key already exists in the map, the value is updated and output log.
      if (rtnMap.containsKey(lowerCaseKey)) {
        detailLog.warn("html key is dupliicated in options. Duplicated key: " + key);
      }

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

  /**
   * Returns value obtained from the key.
   * 
   * @param options options
   * @param key key
   * @return value
   */
  public String getValue(String options, String key) {
    return optionMap(options).get(key.toLowerCase());
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
}
