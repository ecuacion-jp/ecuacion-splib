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

public class HtmlFieldNumber extends HtmlField {

  /** 文字列項目の場合のみ使用可。検索項目の文字列検索での大文字・小文字の区別を指定。 */
  protected boolean needsCommas;

  public HtmlFieldNumber(String id) {
    super(id);
  }

  @Override
  public HtmlFieldNumber displayNameId(String displayNameId) {
    return (HtmlFieldNumber) super.displayNameId(displayNameId);
  }

  @Override
  public HtmlFieldNumber isNotEmpty(boolean isNotEmpty) {
    return (HtmlFieldNumber) super.isNotEmpty(isNotEmpty);
  }

  @Override
  public HtmlFieldNumber isNotEmpty(HtmlFieldConditionKeyEnum authKind, String authString,
      boolean isNotEmpty) {
    return (HtmlFieldNumber) super.isNotEmpty(authKind, authString, isNotEmpty);
  }

  public HtmlFieldNumber needsCommas(boolean needsCommas) {
    this.needsCommas = needsCommas;
    return this;
  }

  public boolean getNeedsCommas() {
    return needsCommas;
  }
}
