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
package jp.ecuacion.splib.web.form.enums;

public enum BooleanOnSearchEnum {
  
  ALL("0"), TRUE("1"), FALSE("2");

  private String code;

  private BooleanOnSearchEnum(String code) {
    this.code = code;
  }
  
  public String getCode() {
    return code;
  }
  
  public static BooleanOnSearchEnum getEnumFromCode(String code) {
    // 画面から受け取る値がnullや""の場合はALLとして扱う
    code = code == null || code.equals("") ? "0" : code;
    
    for (BooleanOnSearchEnum anEnum : BooleanOnSearchEnum.values()) {
      if (anEnum.getCode().equals(code)) {
        return anEnum;
      }
    }
    
    // ここまでくるのは不正の値を引数に指定した場合のみ。
    throw new RuntimeException("[account-book-admin] Program Incorrect.");
  }
  
  public Boolean getSearchValue() {
    
    if (this == ALL) {
      return null;
      
    } else if (this == TRUE) {
      return true;
      
    } else {
      return false;
    }
  }
}
