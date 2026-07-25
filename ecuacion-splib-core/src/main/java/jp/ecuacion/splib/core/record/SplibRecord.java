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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

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
    // Use default.
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
  public @Nullable Integer getStringLength(String itemName) {
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
  public @Nullable Object getValue(String itemPropertyPath) {
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

        rtn = relationRec.getValue(subPropertyPath);

      } else {
        // The case that the value which is wanted to obtain is hold in this record.
        Method m = this.getClass().getMethod("get" + StringUtils.capitalize(itemPropertyPath));
        rtn = m.invoke(this);
      }

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }

    return rtn;
  }

  /**
   * Returns boolean dropdown list.
   */
  protected List<String[]> getBooleanDropdownList(Locale locale, String fieldName, String options) {
    List<String[]> rtnList = new ArrayList<>();

    Arrays.asList(new String[] {"true", "false"}).stream().forEach(bl -> rtnList.add(new String[] {
        bl, PropertiesFileUtil.getMessage(locale, "boolean." + fieldName + "." + bl)}));

    return rtnList;
  }

  /**
   * Holds a snapshot of this record's own and related records' IDs, joined with ",".
   *
   * <p>This is used to identify which DB record(s) this record corresponds to across a
   *     screen-transition round trip (e.g. list screen to edit screen), not to hold the live,
   *     currently-selected value of a relation. It is populated once when the record is built
   *     from a DB entity (see the entity-arg constructor), and carried through hidden form
   *     fields as-is. It is intentionally independent of the entity's own id fields
   *     ({@code getId()}/{@code setId()} and relation equivalents) so that it never conflicts
   *     with a directly-bound, user-editable field for the same relation.</p>
   */
  private String ids = "";

  /**
   * Holds a snapshot of this record's own and related records' optimistic lock versions, joined
   * with ",", in the same order as {@link #ids}.
   *
   * <p>Used together with {@link #ids} for {@code findAndOptimisticLockingCheck()}. See
   *     {@link #ids} for why this is independent of the entity's own version fields.</p>
   */
  private String optimisticLockVersions = "";

  public String getIds() {
    return ids;
  }

  public void setIds(String ids) {
    this.ids = ids;
  }

  public String getOptimisticLockVersions() {
    return optimisticLockVersions;
  }

  public void setOptimisticLockVersions(String optimisticLockVersions) {
    this.optimisticLockVersions = optimisticLockVersions;
  }

  /**
   * Extracts one ","-separated segment from an {@code ids}/{@code optimisticLockVersions}
   * snapshot string.
   *
   * <p>"," is used rather than "-" because manually-created records (e.g. seed data) are often
   *     given a negative id/version on purpose to avoid colliding with the DB sequence, and "-"
   *     as a separator would then collide with the leading minus sign.</p>
   *
   * @param csv the snapshot string ({@link #getIds()} or {@link #getOptimisticLockVersions()})
   * @param index 0 for this record's own value, 1 and above for related records' values in the
   *     order they were joined
   * @return the segment value, or {@code ""} if the snapshot doesn't have that many segments
   */
  protected static String getSnapshotSegment(String csv, int index) {
    String[] segments = csv.split(",", -1);
    return index < segments.length ? segments[index] : "";
  }
}
