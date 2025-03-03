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
package jp.ecuacion.splib.core.form.record;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides a store which holds info about single something.
 * 
 * <p>Records are included in form.</p>
 */
public abstract class SplibRecord {

  protected DatetimeFormatParameters dateTimeFormatParams;

  static final Map<String, Integer> stringLengthMap = new HashMap<>();

  /**
   * Constructs a new instance.
   */
  public SplibRecord() {
    // defaultを使用
    dateTimeFormatParams = new DatetimeFormatParameters();
  }

  /**
   * Constructs a new instance.with {@code DatetimeFormatParameters}.
   * 
   * @param params {@code DatetimeFormatParameters}
   */
  public SplibRecord(DatetimeFormatParameters params) {
    this.dateTimeFormatParams = params;
  }

  /**
   * Gets value.
   * 
   * @param itemName itemName
   * @return Object
   */
  public Object getValue(String itemName) {
    Object rtn = null;

    try {
      if (itemName.contains(".")) {
        String recordName = itemName.substring(0, itemName.indexOf("."));
        String fieldName = itemName.substring(itemName.indexOf(".") + 1);

        Method m = this.getClass().getMethod("get" + StringUtils.capitalize(recordName));

        // そもそもrelationのrecordの値がnullの場合は、それにgetValueするとNullPointerになるのでその前に返す
        SplibRecord relationRec = (SplibRecord) m.invoke(this);
        if (relationRec == null) {
          return null;
        }

        rtn = (relationRec).getValue(fieldName);

      } else {
        Method m = this.getClass().getMethod("get" + StringUtils.capitalize(itemName));
        // Stringの場合とSplibJpaRecordの場合があるが、Objectで返すだけなので条件分岐は不要。
        rtn = m.invoke(this);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return rtn;
  }

  public DatetimeFormatParameters getDateTimeFormatParams() {
    return dateTimeFormatParams;
  }

  public void setDateTimeFormatParams(DatetimeFormatParameters dateTimeFormatParams) {
    this.dateTimeFormatParams = dateTimeFormatParams;
  }

  /**
   * Gets string length.
   * 
   * @param itemName itemName
   * @return Integer
   */
  public Integer getStringLength(String itemName) {
    return stringLengthMap.get(itemName);
  }

  protected static Map<String, Integer> getStringLengthMap() {
    return stringLengthMap;
  }
}
