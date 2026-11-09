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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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

  /** Holds string lengths registered through the deprecated {@link #getStringLengthMap()}. */
  static final Map<String, Integer> stringLengthMap = new HashMap<>();

  /**
   * Holds string lengths per class registered through {@link #getStringLengthMap(Class)}.
   *
   * <p>The inner maps allow {@code null} values
   *     (code-generator registers {@code null} for some columns), so they're not
   *     {@code ConcurrentHashMap}.</p>
   */
  private static final Map<Class<?>, Map<String, @Nullable Integer>> stringLengthMapByClass =
      new ConcurrentHashMap<>();

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
   * Gets the string length of the item, which is used as the {@code maxlength} attribute
   *     of the html input components.
   *
   * <p>The class which holds the field is resolved by following {@code itemPropertyPath}
   *     from the class of this record, using the declared (not runtime) types of the getters.
   *     A segment can have an index like {@code rows[3]} or {@code rows[]},
   *     whose element type is taken from the type argument of the collection or the array.
   *     Then the length is looked up from that class up to its superclasses
   *     in the map registered with {@link #getStringLengthMap(Class)}.</p>
   *
   * <p>When the length isn't found that way,
   *     the value registered with the deprecated {@link #getStringLengthMap()}
   *     for the last field name is returned for backward compatibility.</p>
   *
   * @param itemPropertyPath the itemPropertyPath from this record, like {@code name},
   *     {@code acc.name} or {@code nameRows[3].name}
   * @return the string length, {@code null} if not registered
   */
  public @Nullable Integer getStringLength(String itemPropertyPath) {
    String[] segments = itemPropertyPath.split("\\.");
    String fieldName = removeIndex(segments[segments.length - 1]);

    Class<?> cls = getClass();
    for (int i = 0; i < segments.length - 1; i++) {
      Class<?> next = getPropertyClass(cls, segments[i]);
      if (next == null) {
        return stringLengthMap.get(fieldName);
      }

      cls = next;
    }

    initialize(cls);
    for (@Nullable Class<?> c = cls; c != null; c = c.getSuperclass()) {
      @Nullable Map<String, @Nullable Integer> map = stringLengthMapByClass.get(c);
      if (map != null && map.containsKey(fieldName)) {
        return map.get(fieldName);
      }
    }

    return stringLengthMap.get(fieldName);
  }

  /**
   * Gets the map to register string lengths without distinguishing classes.
   *
   * <p>Since the map is shared by all the records, a length registered here is overwritten
   *     when another record registers a field with the same name.</p>
   *
   * @return the map
   * @deprecated Use {@link #getStringLengthMap(Class)} instead.
   */
  @Deprecated
  protected static Map<String, Integer> getStringLengthMap() {
    return stringLengthMap;
  }

  /**
   * Gets the map to register string lengths of the fields of {@code cls}.
   *
   * <p>Register the lengths in the static initializer of the record like
   *     {@code getStringLengthMap(XxxRecord.class).put("name", 50)}.
   *     The lengths are also applied to the subclasses of {@code cls}.</p>
   *
   * <p>{@code cls} doesn't need to be a {@code SplibRecord}.
   *     For the classes of the rows in a list of a record (like the rows of rowAddableTable),
   *     register the lengths in the static initializer of the record which has the list,
   *     like {@code getStringLengthMap(XxxRow.class).put("name", 50)}.</p>
   *
   * @param cls the class which has the fields
   * @return the map whose keys are field names and values are string lengths
   */
  protected static Map<String, @Nullable Integer> getStringLengthMap(Class<?> cls) {
    return Objects.requireNonNull(stringLengthMapByClass.computeIfAbsent(cls,
        k -> Collections.synchronizedMap(new HashMap<>())));
  }

  /**
   * Gets the declared type of the property, or the element type when the segment has an index.
   *
   * @return the class, {@code null} if it can't be resolved
   */
  private static @Nullable Class<?> getPropertyClass(Class<?> cls, String segment) {
    boolean hasIndex = segment.contains("[");
    String name = removeIndex(segment);

    Type type;
    try {
      type = cls.getMethod("get" + StringUtils.capitalize(name)).getGenericReturnType();

    } catch (NoSuchMethodException e) {
      return null;
    }

    if (hasIndex) {
      if (type instanceof ParameterizedType pt && pt.getActualTypeArguments().length == 1) {
        type = pt.getActualTypeArguments()[0];

      } else if (type instanceof Class<?> c && c.isArray()) {
        return c.getComponentType();

      } else {
        return null;
      }
    }

    if (type instanceof Class<?> c) {
      return c;

    } else if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) {
      return c;
    }

    return null;
  }

  private static String removeIndex(String segment) {
    int index = segment.indexOf('[');
    return index < 0 ? segment : segment.substring(0, index);
  }

  /**
   * Runs the static initializer of {@code cls} if it hasn't run yet,
   *     since the lengths are registered there.
   */
  private static void initialize(Class<?> cls) {
    try {
      Class.forName(cls.getName(), true, cls.getClassLoader());

    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
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
   * with ",", in the same order as {@link #ids}, carried through hidden form fields as-is (see
   * the entity-arg constructor).
   *
   * <p>Unlike {@link #ids}, a version has no legitimate use as a directly-bound, user-editable
   *     value - optimistic-lock versions are a purely internal mechanism the user never sets. So
   *     generated {@code Record} subclasses override {@code setOptimisticLockVersions()} to
   *     cascade each "," segment straight into this record's own and every related record's live
   *     {@code version} field on bind, rather than keeping it as an independent value read back
   *     out through a separate per-relation snapshot accessor the way {@link #ids} does.</p>
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
