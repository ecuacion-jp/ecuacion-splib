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
package jp.ecuacion.splib.web.form.record;

import jp.ecuacion.splib.core.form.record.SplibRecord;
import jp.ecuacion.splib.web.bean.HtmlItem;

/**
 * Is a record for loginController.
 */
public class LoginRecord extends SplibRecord implements RecordInterface {

  @Override
  public HtmlItem[] getHtmlItems() {
    return new HtmlItem[] {};
  }

  private String username;
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * username length.
   * 
   * @return long
   */
  public long usernameLength() {
    return 20;
  }

  /**
   * password length.
   */
  public long passwordLength() {
    return 20;
  }
}
