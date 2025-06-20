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
package jp.ecuacion.splib.web.bean;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.lib.core.annotation.RequireNonempty;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;
import org.apache.commons.lang3.StringUtils;

/**
 * Stores attributes of each html component which controls the behaviors of it in html pages.
 * 
 * <p>{@code HtmlItem} is a kind of "item". See <a href="https://github.com/ecuacion-jp/ecuacion-jp.github.io/blob/main/documentation/common/naming-convention.md">naming-convention.md</a>
 *     So its ID should consist of itemKindIdClass and itemKindIdField.<br>
 *     It complies the rule, but {@code itemKindIdClass} can be empty.
 *     When it's empty, 
 *     {@code mainRootRecord} in {@code Form} is used as itemKindIdClass as default.</p>
 *     
 * <p>Actually itemKindId cannot designate a unique location. 
 *     There can be multiple location as a relation or relation of relation.
 *     To have a way to pinpoint the location of the form, it also has {@code propertyPath}.
 *     It can be empty for normal use, but you can make differences when you want.</p>
 */
public class HtmlItem {

  /**
   * Is the ID string of htmlItem.
   * 
   * <p>rootRecordName part (= far left part) can be omitted. Namely it can be "name" or "dept.name"
   *     when the real propertyPath is "acc.name" or "acc.dept.name" 
   *     where "acc" is the rootRecordName.</p>
   */
  @Nonnull
  protected String itemPropertyPath;

  /**
   * Is a class part (= left part) of itemKindId. (like "acc" from itemKindId: "acc.name")
   * 
   * <p>It can be {@code null}, then {@code rootRecordName} obtained from context is used.</p>
   */
  protected String itemKindIdClass;

  /**
   * Is a field part (= right part) of itemKindId. (like "name" from itemKindId: "acc.name")
   */
  @Nonnull
  protected String itemKindIdField;

  /**
   * Is a class part (= left part) of itemNameKey. (like "acc" from itemKindId: "acc.name")
   * 
   * <p>The display name can be obtained by referring {@code item_names.properties} with it.</p>
   */
  protected String itemNameKeyClass;

  /**
   * Is a field part (= right part) of itemNameKey. (like "name" from itemKindId: "acc.name")
   * 
   * <p>The display name can be obtained by referring {@code item_names.properties} with it.</p>
   */
  protected String itemNameKeyField;

  /**
   * Shows whether the field allows empty.
   * 
   * <p>When this value is {@code true}, required validation is executed on server side,
   *     and shows "required" mark on the component in the html page.</p>
   * 
   * <p>It doesn't depend on the data type.</p>
   * 
   * <p>The default value is preset: {@code false}. So the value becomes {@code false} 
   *     if you don't have to set this value.</p>
   */
  protected HtmlItemConditionContainer<Boolean> isNotEmpty =
      new HtmlItemConditionContainer<>(false);

  /**
   * Constructs a new instance with {@code ID}.
   * 
   * <p>Generally propertyPath starts with a rootRecord 
   *     (a record field name which is directly defined in a form)
   *     and should be like {@code user.name} or {@code user.dept.name}.
   *     But it is also allowed that you can omit the top rootRecord part 
   *     when {@code user} is the mainRootRecord, 
   *     which means {@code name} or {@code dept.name} is also okay.
   *     When creating html htmlItems are searched with propertyPath,
   *     and if nothing found, 
   *     mainRootRecord is automatically added to the top of propertyPath and search again.</p>
   * 
   * @param itemPropertyPath propertyPath, maybe without top mainRootRecord part.
   *     (when "user" is mainRootRecord, both user.name and name are accepted)
   */
  public HtmlItem(@RequireNonempty String itemPropertyPath) {

    this.itemPropertyPath = ObjectsUtil.requireNonEmpty(itemPropertyPath);

    // Remove far left part ("name" in "acc.name") from propertyPath.
    // It's null when propertyPath doesn't contain ".".
    String propertyPathWithoutFarRightPart = itemPropertyPath.contains(".")
        ? itemPropertyPath.substring(0, itemPropertyPath.lastIndexOf("."))
        : null;

    this.itemKindIdClass = propertyPathWithoutFarRightPart == null ? null
        : (propertyPathWithoutFarRightPart.contains(".")
            ? propertyPathWithoutFarRightPart.substring(itemPropertyPath.lastIndexOf(".") + 1)
            : propertyPathWithoutFarRightPart);
    this.itemKindIdField = itemPropertyPath.contains(".")
        ? itemPropertyPath.substring(itemPropertyPath.lastIndexOf(".") + 1)
        : itemPropertyPath;
  }

  public String getItemPropertyPath() {
    return itemPropertyPath;
  }

  /**
   * Returns itemKindId.
   * 
   * <p>itemKindId is built from itemKindIdClass and itemKindIdField.<br>
   * When itemKindIdClass is {@code null}, rootRecordName is used instead.</p>
   * 
   * @param rootRecordName rootRecordName
   * @return itemKindId
   */
  public String getItemKindId(String rootRecordName) {
    return (StringUtils.isEmpty(itemKindIdClass) ? rootRecordName : itemKindIdClass) + "."
        + itemKindIdField;
  }

  /**
   * Sets {@code itemNameKey} and returns this for method chain.
   * 
   * <p>The format of itemNameKey is the same as itemKindId, which is like "acc.name",
   *     always has one dot (not more than one) in the middle of the string.<br>
   *     But the argument of the method can be like "name".
   *     In that case itemKindIdClass is used instead. 
   *     When itemKindIdClass is {@code null}, rootRecordName is used instead.</p>
   * 
   * @param itemNameKey itemNameKey
   * @return HtmlItem
   */
  public HtmlItem itemNameKey(@RequireNonempty String itemNameKey) {
    ObjectsUtil.requireNonEmpty(itemNameKey);

    this.itemNameKeyClass =
        itemNameKey.contains(".") ? itemNameKey.substring(0, itemNameKey.lastIndexOf(".")) : null;
    this.itemNameKeyField =
        itemNameKey.contains(".") ? itemNameKey.substring(itemNameKey.lastIndexOf(".") + 1)
            : itemNameKey;
    return this;
  }

  /**
   * Returns {@code itemNameKey} value.
   * 
   * <p>Its value is {@code null} means 
   *     the item's original itemKindId is equal to itemNameKey.</p>
   * 
   * @return itemKindIdFieldForName
   */
  public String getItemNameKey(String rootRecordName) {
    String classPart = !StringUtils.isEmpty(itemNameKeyClass) ? itemNameKeyClass
        : (!StringUtils.isEmpty(itemKindIdClass) ? itemKindIdClass : rootRecordName);
    String fieldPart = !StringUtils.isEmpty(itemNameKeyField) ? itemNameKeyField : itemKindIdField;

    return classPart + "." + fieldPart;
  }

  /**
   * Sets isNotEmpty.
   * 
   * <p>Set {@code true} when you want to the item is required.</p>
   * 
   * @param isNotEmpty isNotEmpty
   * @return HtmlItem
   */
  public HtmlItem isNotEmpty(boolean isNotEmpty) {
    this.isNotEmpty.setDefaultValue(isNotEmpty);
    return this;
  }

  /**
   * Sets isNotEmpty with the conditions of {@code HtmlItemConditionKeyEnum}, {@code authString}.
   * 
   * <p>When you set multiple conditions to it, the order matters. First condition prioritized.</p>
   * 
   * @param authKind authKind
   * @param authString authString
   * @param isNotEmpty isNotEmpty
   * @return HtmlItem
   */
  public HtmlItem isNotEmpty(HtmlItemConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    this.isNotEmpty.add(new HtmlItemCondition<Boolean>(authKind, authString, isNotEmpty));
    return this;
  }

  /**
   * Obtains isNotEmpty.
   * 
   * @param loginState loginState
   * @param bean bean
   * @return boolean
   */
  public boolean getIsNotEmpty(String loginState, RolesAndAuthoritiesBean bean) {
    return isNotEmpty.getValue(loginState, bean);
  }

  /**
   * Is the condition which decides isNotEmpty or not.
   */
  public static enum HtmlItemConditionKeyEnum {
    LOGIN_STATE, ROLE, AUTHORITY, KEYWORD;
  }

  /**
   * Stores multiple HtmlItem conditions.
   * 
   * <p>Conditions are stored in {@code List}, so the order matters.<br>
   *     If you set {@code new HtmlItem("name").setXxx(KEYWORD, "update", "A")
   *     .setXxx(ROLE, "admin", "B").setXxx("X")} 
   *     and parameters have keyword update and role "admin",
   *     the resultant value becomes "A".<br>
   *     .setXxx("C") sets the default value so even if you set as follows
   *     and you give a parameter keyword "update", the resultant value is "A".<br>
   *     {@code new HtmlItem("name").setXxx("X").setXxx(KEYWORD, "update", "A")} 
   *     </p>
   * 
   * @param <T> data type of the value
   */
  public static class HtmlItemConditionContainer<T> {
    private List<HtmlItemCondition<T>> list = new ArrayList<>();
    private T defaultValue;

    /**
     * Returns the defaultValue.
     * 
     * @param defaultValue defaultValue
     */
    public HtmlItemConditionContainer(T defaultValue) {
      this.defaultValue = defaultValue;
    }

    /**
     * adds condition.
     * 
     * @param authInfo authInfo
     */
    public void add(HtmlItemCondition<T> authInfo) {
      getList().add(authInfo);
    }

    private List<HtmlItemCondition<T>> getList() {
      return list;
    }

    public void setList(List<HtmlItemCondition<T>> list) {
      this.list = list;
    }

    public T getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
      this.defaultValue = defaultValue;
    }

    /**
     * Returns value.
     * 
     * @param loginState loginState
     * @param bean bean
     * @return T
     */
    public T getValue(String loginState, RolesAndAuthoritiesBean bean) {
      if (list == null) {
        list = new ArrayList<>();
      }

      for (HtmlItemCondition<T> info : list) {
        if (info.getConditionKey() == HtmlItemConditionKeyEnum.LOGIN_STATE) {
          if (info.getConditionValue().equals(loginState)) {
            return info.getValue();
          }

        } else if (info.getConditionKey() == HtmlItemConditionKeyEnum.ROLE) {
          if (bean.getRoleList().contains(info.getConditionValue())) {
            return info.getValue();
          }

        } else if (info.getConditionKey() == HtmlItemConditionKeyEnum.AUTHORITY) {
          if (bean.getAuthorityList().contains(info.getConditionValue())) {
            return info.getValue();
          }

        } else if (info.getConditionKey() == HtmlItemConditionKeyEnum.KEYWORD) {
          if (bean.getAuthorityList().contains(info.getConditionValue())) {
            return info.getValue();
          }

        } else {
          throw new RuntimeException("Set appropriate one of the AuthKindEnum values");
        }
      }

      return defaultValue;
    }
  }

  /**
   * Stores one HtmlItem condition. See {@link HtmlItemConditionContainer}.
   * 
   * @param <T> data type of the value
   */
  public static class HtmlItemCondition<T> {
    private HtmlItemConditionKeyEnum conditionKey;
    private String conditionValue;
    private T value;

    /**
     * Constructs a new instance.
     * 
     * @param conditionKey conditionKey
     * @param conditionValue conditionValue
     * @param value value
     */
    public HtmlItemCondition(HtmlItemConditionKeyEnum conditionKey, String conditionValue,
        T value) {
      this.conditionKey = conditionKey;
      this.conditionValue = conditionValue;
      this.value = value;
    }

    public HtmlItemConditionKeyEnum getConditionKey() {
      return conditionKey;
    }

    public String getConditionValue() {
      return conditionValue;
    }

    public T getValue() {
      return value;
    }
  }
}
