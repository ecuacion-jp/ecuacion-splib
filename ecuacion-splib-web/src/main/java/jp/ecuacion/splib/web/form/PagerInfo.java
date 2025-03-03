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
 * Consists of a cell which is a part of a pager structure.
 */
public class PagerInfo {

  /** The string displaying on the cell, like "Next". */
  private String displayString;
  
  /**
   * Stores the status of the cell with the format of the string used in bootstrap.
   * 
   * <p>The values are either "active" or "disabled".</p>
   */
  private String status;
  
  /**
   * The page number to which the page transitions by pressing a link.
   */
  private String goToPage;

  /**
   * Constructs a new instance with disable cell.
   */
  public PagerInfo(String displayString) {
    this.displayString = displayString;
    this.status = "disabled";
  }
  
  /**
   * Constructs a new instance with clickable cell.
   */
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
