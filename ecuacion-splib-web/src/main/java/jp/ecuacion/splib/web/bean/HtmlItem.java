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
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;

/**
 * Stores attributes of each html component which controls the behaviors of it in html pages.
 * 
 * <p>{@code HtmlItem} is a kind of "item". See <a href="https://github.com/ecuacion-jp/ecuacion-jp.github.io/blob/main/documentation/common/naming-convention.md">naming-convention.md</a>
 *     So its ID should consist of itemIdClass and itemIdField.<br>
 *     It complies the rule, but {@code itemIdClass} can be empty.
 *     When it's empty, {@code mainRootRecord} in {@Form} is used as itemIdClass as default.</p>
 *     
 * <p>Actually itemId cannot designate a unique location. 
 *     There can be multiple location as a relation or relation of relation.
 *     To have a way to pinpoint the location of the form, it also has {@code propertyPath}.
 *     It can be empty for normal use, but you can make differences when you want.</p>
 */
public class HtmlItem {

  /**
   * propertyPath.
   */
  protected String propertyPath;

  /**
   * Is an ID which the root record name plus dot (like {@code "acc."}) is removed
   * from the itemId.
   */
  @Nonnull
  protected String itemIdField;

  /**
   * Is an itemIdField used to display the name of the item.
   * 
   * <p>The display name can be obtained by referring {@code item_names.properties} with it.</p>
   */
  protected String itemIdFieldForName;

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
   *     It is allowed that you can omit the top rootRecord part 
   *     when {@code user} is the mainRootRecord, 
   *     which means {@code name} or {@code dept.name} is also okay.
   *     When creating html htmlItems are searched with propertyPath,
   *     and if nothing found, 
   *     mainRootRecord is automatically added to the top of propertyPath and search again.</p>
   * 
   * @param propertyPath propertyPath, maybe without top mainRootRecord part.
   *     (when "user" is mainRootRecord, both user.name and name are accepted)
   */
  public HtmlItem(String propertyPath) {
    this.propertyPath = propertyPath;
    this.itemIdField = propertyPath;
  }
  
  public String getPropertyPath() {
    return propertyPath;
  }

  public String getItemIdField() {
    return itemIdField;
  }

  /**
   * Sets {@code itemIdFieldForName} and returns this for method chain.
   * 
   * @param itemIdFieldForName itemIdFieldForName
   * @return HtmlItem
   */
  public HtmlItem itemIdFieldForName(String itemIdFieldForName) {
    this.itemIdFieldForName = itemIdFieldForName;
    return this;
  }

  /**
   * Returns {@code itemIdFieldForName} value.
   * 
   * <p>Its value is {@code null} means 
   *     the item's original itemId is equal to itemIdFieldForName.</p>
   * 
   * @return itemIdFieldForName
   */
  public String getItemIdFieldForName() {
    return itemIdFieldForName;
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
