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
 *     because it's name is defined in the "field" format like {@code "mailAddress"}.</p>
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
   * <p>The display name can be obtained by referring {@code field_names.properties} with it.</p>
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

  public HtmlField displayNameId(String displayNameId) {
    this.displayNameId = displayNameId;
    return this;
  }

  /**
   * Returns {@code displayNameId} value if it's not {@code null}, 
   *     and 
   * 
   * @return
   */
  public String getDisplayNameId() {
    return displayNameId;
  }

  public HtmlField isNotEmpty(boolean isNotEmpty) {
    this.isNotEmpty.setDefaultValue(isNotEmpty);
    return this;
  }

  /** 複数登録する場合は、登録順序に意味がある。先に登録されたものから順に検証し、当てはまればその値を使用する。 */
  public HtmlField isNotEmpty(HtmlFieldConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    this.isNotEmpty.add(new HtmlFieldCondition<Boolean>(authKind, authString, isNotEmpty));
    return this;
  }

  public boolean getIsNotEmpty(String loginState, RolesAndAuthoritiesBean bean) {
    return isNotEmpty.getValue(loginState, bean);
  }

  /**
   * 
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

    public HtmlFieldConditionContainer(T defaultValue) {
      this.defaultValue = defaultValue;
    }

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
