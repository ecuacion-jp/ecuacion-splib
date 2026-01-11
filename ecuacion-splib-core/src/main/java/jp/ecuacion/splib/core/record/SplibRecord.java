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
package jp.ecuacion.splib.core.record;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jp.ecuacion.lib.core.exception.unchecked.EclibRuntimeException;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
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

  /**
   * Gets value from itemPropertyPath.
   * 
   * @param itemPropertyPath itemName
   * @return Object
   */
  public Object getValue(String itemPropertyPath) {
    Object rtn = null;

    try {
      if (itemPropertyPath.contains(".")) {
        String fieldName = itemPropertyPath.substring(0, itemPropertyPath.indexOf("."));
        String subPropertyPath = itemPropertyPath.substring(itemPropertyPath.indexOf(".") + 1);

        Method m = this.getClass().getMethod("get" + StringUtils.capitalize(fieldName));

        // In the case of relationRec == null NullPointerException occurs
        // when getValue method is called, so return null before it happens in that case.
        SplibRecord relationRec = (SplibRecord) m.invoke(this);
        if (relationRec == null) {
          return null;
        }

        rtn = (relationRec).getValue(subPropertyPath);

      } else {
        // The case that the value which is wanted to obtain is hold in this record.
        Method m = this.getClass().getMethod("get" + StringUtils.capitalize(itemPropertyPath));
        rtn = m.invoke(this);
      }

    } catch (Exception e) {
      throw new EclibRuntimeException(e);
    }

    return rtn;
  }

  /**
   * Returns boolean dropdown list.
   */
  protected List<String[]> getBooleanDropdownList(Locale locale, String fieldName, String options) {
    List<String[]> rtnList = new ArrayList<>();

    Arrays.asList(new String[] {"true", "false"}).stream().forEach(bl -> rtnList.add(
        new String[] {bl, PropertyFileUtil.getMessage(locale, "boolean." + fieldName + "." + bl)}));

    return rtnList;
  }
}
