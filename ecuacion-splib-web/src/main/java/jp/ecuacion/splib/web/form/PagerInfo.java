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
package jp.ecuacion.splib.web.form;

/** 
 * Pagerを構成する1セルの情報が1PageInfo。
 */
public class PagerInfo {

  /** セルに表示する文字列。"Next"など。 */
  private String displayString;
  
  /** そのセルの状態を、bootstrapで使用する文字列でそのまま表現。"", "active", "disabled" のいずれか。 */
  private String status;
  
  /** リンクを押した時に飛ぶ先のページ番号。 */
  private String goToPage;
  
  /** disabledのセルを生成する際に使用するconstructor. */
  public PagerInfo(String displayString) {
    this.displayString = displayString;
    this.status = "disabled";
  }
  
  /** clickableセルを生成する際に使用するconstructor. */
  public PagerInfo(String displayString, boolean isActive, int goToPage) {
    this.displayString = displayString;
    this.status = (isActive) ? "active" : "";
    this.goToPage = Integer.toString(goToPage);
  }

  public String getDisplayString() {
    return displayString;
  }

  public void setDisplayString(String displayString) {
    this.displayString = displayString;
  }
  
  public String getStatus() {
    return status;
  }
  
  public void setStatus(String status) {
    this.status = status;
  }
  
  public boolean isDisabled() {
    return status.equals("disabled");
  }
  
  public String getGoToPage() {
    return goToPage;
  }
  
  public void setGoToPage(String goToPage) {
    this.goToPage = goToPage;
  }
}
