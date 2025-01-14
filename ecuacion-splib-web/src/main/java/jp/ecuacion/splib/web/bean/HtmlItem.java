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

import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.splib.web.util.SplibSecurityUtil.RolesAndAuthoritiesBean;

/**
 * Stores attributes of each field which controls the behiviors of the field in a record.
 */
public class HtmlItem {

  /**
   * Is a fieldName which the root record name plus dot (like (accGroup.) is removed.
   * 
   * <p>When you treat the field in the record belonging to the root record, 
   * you need to define it like {@code childRecord.childFieldName}.</p>
   */
  protected String itemName;

  /**
   * Is a key to the label of the field.
   * 
   * <p>The root record name plus dot (like (accGroup.) is removed.<br>
   *     See {@link jp.ecuacion.splib.web.form.record.RecordInterface#getLabelItemName(
   *     String, String)}.</p>
   */
  protected String labelItemName;

  /** 登録・更新時に入力必須かを示す。itemの種類によらず共通。 */
  protected AuthInfoContainer<Boolean> isNotEmpty = new AuthInfoContainer<>(false);

  /** method chainでの設定を行う場合に使用。 */
  public HtmlItem(String itemName) {
    this.itemName = itemName;
  }

  public String getItemName() {
    return itemName;
  }

  public HtmlItem labelItemName(String labelItemName) {
    this.labelItemName = labelItemName;
    return this;
  }

  public String getLabelItemName() {
    return labelItemName;
  }

  public HtmlItem isNotEmpty(boolean isNotEmpty) {
    this.isNotEmpty.setDefaultValue(isNotEmpty);
    return this;
  }

  /** 複数登録する場合は、登録順序に意味がある。先に登録されたものから順に検証し、当てはまればその値を使用する。 */
  public HtmlItem isNotEmpty(AuthKindEnum authKind, String authString, boolean isNotEmpty) {
    this.isNotEmpty.add(new AuthInfo<Boolean>(authKind, authString, isNotEmpty));
    return this;
  }

  public boolean getIsNotEmpty(String loginState, RolesAndAuthoritiesBean bean) {
    return isNotEmpty.getValue(loginState, bean);
  }

  public static enum AuthKindEnum {
    loginState, role, authority;
  }

  /**
   * 
   * 
   * @param <T>
   */
  public static class AuthInfoContainer<T> {
    private List<AuthInfo<T>> list = new ArrayList<>();
    private T defaultValue;

    public AuthInfoContainer(T defaultValue) {
      this.defaultValue = defaultValue;
    }

    public void add(AuthInfo<T> authInfo) {
      getList().add(authInfo);
    }

    public List<AuthInfo<T>> getList() {
      return list;
    }

    public void setList(List<AuthInfo<T>> list) {
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

      for (AuthInfo<T> info : list) {
        if (info.getAuthKind() == AuthKindEnum.loginState) {
          if (info.getAuthString().equals(loginState)) {
            return info.getValue();
          }

        } else if (info.getAuthKind() == AuthKindEnum.role) {
          if (bean.getRoleList().contains(info.getAuthString())) {
            return info.getValue();
          }

        } else if (info.getAuthKind() == AuthKindEnum.authority) {
          if (bean.getAuthorityList().contains(info.getAuthString())) {
            return info.getValue();
          }

        } else {
          throw new RuntimeException("Set appropriate one of the AuthKindEnum values");
        }
      }

      return defaultValue;
    }
  }

  public static class AuthInfo<T> {
    private AuthKindEnum authKind;
    private String authString;
    private T value;

    public AuthInfo(AuthKindEnum authKind, String authString, T value) {
      this.authKind = authKind;
      this.authString = authString;
      this.value = value;
    }

    public AuthKindEnum getAuthKind() {
      return authKind;
    }

    public void setAuthKind(AuthKindEnum authKind) {
      this.authKind = authKind;
    }

    public String getAuthString() {
      return authString;
    }

    public void setAuthString(String authString) {
      this.authString = authString;
    }

    public T getValue() {
      return value;
    }

    public void setValue(T value) {
      this.value = value;
    }
  }
}
