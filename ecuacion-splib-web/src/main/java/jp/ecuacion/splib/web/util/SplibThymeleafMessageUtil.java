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

import java.util.Locale;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Provides localized messages called from thymeleaf.
 * 
 * <p>This calls {@code PropertyFileUtil}. 
 *     The existence of the class doesn't prevent you to use #{...}.</p>
 */
@Component("msgUtil")
public class SplibThymeleafMessageUtil {

  /**
   * Constructs a new instance.
   */
  public SplibThymeleafMessageUtil() {}

  /**
   * Returns whether properties files have a message with key.
   * 
   * @param key key
   * @return boolean
   */
  public boolean hasKey(String key) {

    // Return blank when id is empty.
    if (StringUtils.isEmpty(key)) {
      return false;
    }

    boolean hasMsg = PropertyFileUtil.hasMessage(key);
    boolean hasStr = PropertyFileUtil.hasString(key);
    boolean hasItem = PropertyFileUtil.hasItemName(key);

    return hasMsg || hasStr || hasItem;
  }

  /**
   * Gets message or itemName from the specified key.
   * 
   * @param locale locale
   * @param key key
   * @return String
   */
  public String getValue(Locale locale, String key, String... args) {

    // Return blank when id is empty.
    if (StringUtils.isEmpty(key)) {
      return "";
    }

    boolean hasMsg = PropertyFileUtil.hasMessage(key);
    boolean hasStr = PropertyFileUtil.hasString(key);
    boolean hasItem = PropertyFileUtil.hasItemName(key);

    // Return id when id not exist in both.
    if (!hasMsg && !hasStr && !hasItem) {
      return key;
    }

    // Even when hasMsg == false && hasItem = false, Exception doesn't emerge.
    if (hasMsg) {
      return PropertyFileUtil.getMessage(locale, key, args);

    } else if (hasStr) {
      return PropertyFileUtil.getString(key, args);

    } else {
      return PropertyFileUtil.getItemName(locale, key);
    }
  }

  /**
   * Returns value from the argument key.
   * 
   * @param key key
   * @return String
   */
  public String getValueOrElse(Locale locale, String key, String defaultValue, String... args) {
    return hasKey(key) ? getValue(locale, key, args) : defaultValue;
  }
}
