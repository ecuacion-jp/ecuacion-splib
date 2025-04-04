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
 * Stores attributes of each html component which controls the behiviors of it in html pages.
 * 
 * <p>It is named with "field" 
 *     because it's name is defined in the "field" format 
 *     like {@code mailAddress}, not like {@code acc.mailAddress}.</p>
 */
public class HtmlField {

  /**
   * Is an ID which the root record name plus dot (like {@code "acc."}) is removed
   * from the itemId.
   * 
   * <p>When you treat the field in the record belonging to the root record, 
   * you need to define it like {@code childRecord.childFieldName}.</p>
   * 
   * <p>It is required if you construct this class.</p>
   */
  @Nonnull
  protected String id;

  /**
   * Is an ID to the display name of the field.
   * 
   * <p>The display name can be obtained by referring {@code item_names.properties} with it.</p>
   */
  protected String displayNameId;

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
  protected HtmlFieldConditionContainer<Boolean> isNotEmpty =
      new HtmlFieldConditionContainer<>(false);

  /**
   * Constructs a new instance with {@code ID}.
   * 
   * @param id fieldId
   */
  public HtmlField(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  /**
   * Sets {@code displayNameId} and returns this for method chain.
   * 
   * @param displayNameId displayNameId
   * @return HtmlField
   */
  public HtmlField displayNameId(String displayNameId) {
    this.displayNameId = displayNameId;
    return this;
  }

  /**
   * Returns {@code displayNameId} value.
   * 
   * <p>Its value is {@code null} means the item's original itemId is equal to displayNameId.</p>
   * 
   * @return displayNameId
   */
  public String getDisplayNameId() {
    return displayNameId;
  }

  /**
   * Sets isNotEmpty.
   * 
   * <p>Set {@code true} when you want to the item is required.</p>
   * 
   * @param isNotEmpty isNotEmpty
   * @return HtmlField
   */
  public HtmlField isNotEmpty(boolean isNotEmpty) {
    this.isNotEmpty.setDefaultValue(isNotEmpty);
    return this;
  }

  /**
   * Sets isNotEmpty with the conditions of {@code HtmlFieldConditionKeyEnum}, {@code authString}.
   * 
   * <p>When you set multiple conditions to it, the order matters. First condition prioritized.</p>
   * 
   * @param authKind authKind
   * @param authString authString
   * @param isNotEmpty isNotEmpty
   * @return HtmlField
   */
  public HtmlField isNotEmpty(HtmlFieldConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    this.isNotEmpty.add(new HtmlFieldCondition<Boolean>(authKind, authString, isNotEmpty));
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
  public static enum HtmlFieldConditionKeyEnum {
    LOGIN_STATE, ROLE, AUTHORITY, KEYWORD;
  }

  /**
   * Stores multiple HtmlField conditions.
   * 
   * <p>Conditions are stored in {@code List}, so the order matters.<br>
   *     If you set {@code new HtmlField("name").setXxx(KEYWORD, "update", "A")
   *     .setXxx(ROLE, "admin", "B").setXxx("X")} 
   *     and parameters have keyword update and role "admin",
   *     the resultant value becomes "A".<br>
   *     .setXxx("C") sets the default value so even if you set as follows
   *     and you give a parameter keyword "update", the resultant value is "A".<br>
   *     {@code new HtmlField("name").setXxx("X").setXxx(KEYWORD, "update", "A")} 
   *     </p>
   * 
   * @param <T> data type of the value
   */
  public static class HtmlFieldConditionContainer<T> {
    private List<HtmlFieldCondition<T>> list = new ArrayList<>();
    private T defaultValue;

    /**
     * Returns the defaultValue.
     * 
     * @param defaultValue defaultValue
     */
    public HtmlFieldConditionContainer(T defaultValue) {
      this.defaultValue = defaultValue;
    }

    /**
     * adds condition.
     * 
     * @param authInfo authInfo
     */
    public void add(HtmlFieldCondition<T> authInfo) {
      getList().add(authInfo);
    }

    private List<HtmlFieldCondition<T>> getList() {
      return list;
    }

    public void setList(List<HtmlFieldCondition<T>> list) {
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

      for (HtmlFieldCondition<T> info : list) {
        if (info.getConditionKey() == HtmlFieldConditionKeyEnum.LOGIN_STATE) {
          if (info.getConditionValue().equals(loginState)) {
            return info.getValue();
          }

        } else if (info.getConditionKey() == HtmlFieldConditionKeyEnum.ROLE) {
          if (bean.getRoleList().contains(info.getConditionValue())) {
            return info.getValue();
          }

        } else if (info.getConditionKey() == HtmlFieldConditionKeyEnum.AUTHORITY) {
          if (bean.getAuthorityList().contains(info.getConditionValue())) {
            return info.getValue();
          }

        } else if (info.getConditionKey() == HtmlFieldConditionKeyEnum.KEYWORD) {
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
   * Stores one HtmlField condition. See {@link HtmlFieldConditionContainer}.
   * 
   * @param <T> data type of the value
   */
  public static class HtmlFieldCondition<T> {
    private HtmlFieldConditionKeyEnum conditionKey;
    private String conditionValue;
    private T value;

    /**
     * Constructs a new instance.
     * 
     * @param conditionKey conditionKey
     * @param conditionValue conditionValue
     * @param value value
     */
    public HtmlFieldCondition(HtmlFieldConditionKeyEnum conditionKey, String conditionValue,
        T value) {
      this.conditionKey = conditionKey;
      this.conditionValue = conditionValue;
      this.value = value;
    }

    public HtmlFieldConditionKeyEnum getConditionKey() {
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
