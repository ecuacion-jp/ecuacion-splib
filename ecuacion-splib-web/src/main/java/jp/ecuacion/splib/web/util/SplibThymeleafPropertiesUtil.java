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
 * Offers access to application[...].properties. It's supposed to called from thymeleaf.
 * 
 * <p>It calls PropertyFileUtil inside.</p>
 */
@Component("propUtil")
public class SplibThymeleafPropertiesUtil {

  /**
   * Constructs a new instance.
   */
  public SplibThymeleafPropertiesUtil() {}

  /**
   * Returns a boolean whether application[...].properties has the argument key.
   * 
   * @param key key
   * @return boolean
   */
  public boolean hasKey(String key) {
    return PropertyFileUtil.hasApplication(key);
  }

  /**
   * Returns value from the argument key.
   * 
   * @param key key
   * @return String
   */
  public String getValue(String key) {
    return PropertyFileUtil.getApplication(key);
  }

  /**
   * Returns value from the argument key 
   *     or defaultValue when the key does not exist in application[...].properties.
   * 
   * @param key key
   * @return value
   */
  public String getValueOrElse(String key, String defaultValue) {
    return hasKey(key) ? getValue(key) : defaultValue;
  }

  /**
   * Returns loginState dependent value.
   * 
   * <p>When you set 'test.key1' to "key" argument,
   *     it recognize 'test.key1' as a default key, 'test.key1-account' for account loginState
   *     and 'test.key1-admin' for admin loginState.</p>
   * 
   * @param loginState loginState
   * @param key key
   * @param defaultValue defaultValue
   * @return value
   */
  public String getLoginStateDependentValueOrElse(String loginState, String key,
      String defaultValue) {
    String value = getValueOrElse(key, defaultValue);
    String accountValue = getValueOrElse(key + "-account", defaultValue);
    String adminValue = getValueOrElse(key + "-admin", defaultValue);

    return loginState.equals("account") ? accountValue
        : (loginState.equals("admin") ? adminValue : value);
  }
}
