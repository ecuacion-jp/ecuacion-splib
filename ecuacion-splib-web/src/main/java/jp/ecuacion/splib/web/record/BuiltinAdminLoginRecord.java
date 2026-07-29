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
package jp.ecuacion.splib.web.record;

import jp.ecuacion.lib.core.annotation.ItemNameKeyClass;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.web.config.SplibBuiltinAdminSecurityConfig;
import jp.ecuacion.splib.web.item.HtmlItem;
import jp.ecuacion.splib.web.item.HtmlItemContainer;
import org.jspecify.annotations.Nullable;

/**
 * Is a record for {@code BuiltinAdminLoginController}.
 *
 * <p>{@code username} is fixed to
 * {@code SplibBuiltinAdminSecurityConfig.BUILTIN_ADMIN_USERNAME} and shown read-only on the
 * page, but is still a bindable field: {@code components-input :: inputText} always submits a
 * {@code name} attribute for the field it renders, and the security config's authentication
 * still needs a {@code username} request parameter to check against.</p>
 */
@ItemNameKeyClass("builtinAdminLogin")
public class BuiltinAdminLoginRecord extends SplibRecord implements HtmlItemContainer {

  @Override
  public HtmlItem[] customizedItems() {
    return new HtmlItem[] {};
  }

  private String username = SplibBuiltinAdminSecurityConfig.BUILTIN_ADMIN_USERNAME;

  @Nullable
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public @Nullable String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
