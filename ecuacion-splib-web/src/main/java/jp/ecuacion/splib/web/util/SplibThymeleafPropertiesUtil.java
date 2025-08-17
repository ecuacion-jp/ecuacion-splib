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

import jp.ecuacion.lib.core.util.PropertyFileUtil;
import org.springframework.stereotype.Component;

/**
 * thymeleaf側から呼ばれる想定のクラス。localeを加味したメッセージ表示を実施.
 * 
 * <p>裏ではPropertyFileUtilを呼び出しているのみ。PropertyFileUtilの機能が使える分#{...} より協力。 
 *     ただし、#{...}で問題ない部分はそれを使っても問題なし。</p>
 */
@Component("propUtil")
public class SplibThymeleafPropertiesUtil {

  /**
   * Constructs a new instance.
   */
  public SplibThymeleafPropertiesUtil() {}

  /**
   * Returns value from id.
   * 
   * @param id id
   * @return String
   */
  public String get(String id) {
    return PropertyFileUtil.getApplication(id);
  }

  /**
   * Returns value from id.
   * 
   * @param id id
   * @return String
   */
  public boolean has(String id) {
    return PropertyFileUtil.hasApplication(id);
  }

  /**
   * Returns value from id.
   * 
   * @param id id
   * @return String
   */
  public String getValueOrElse(String id, String defaultValue) {
    return has(id) ? get(id) : defaultValue;
  }
}
