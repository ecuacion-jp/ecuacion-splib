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
   * Gets message or itemName from the specified id.
   * 
   * @param locale locale
   * @param id id
   * @return String
   */
  public String get(Locale locale, String id) {
    boolean hasMsg = PropertyFileUtil.hasMessage(id);
    boolean hasItem = PropertyFileUtil.hasItemName(id);

    // 両方に存在する場合はエラー
    if (hasMsg && hasItem) {
      String msg =
          "Key '" + id + "' has both in 'messages.properties' and 'item_names.properties'. "
              + "One of keys must be removed. (key : " + id + ")";
      throw new RuntimeException(msg);
    }

    // Even when hasMsg == false && hasItem = false, Exception doesn't emerge.
    if (hasMsg) {
      return PropertyFileUtil.getMessage(locale, id);

    } else {
      return PropertyFileUtil.getItemName(locale, id);
    }
  }
}
