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

import java.util.HashMap;
import java.util.Map;
import org.springframework.ui.Model;

/** Stores app-common modelAttributes. */
public class SplibModelAttributes extends HashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  private boolean bsBgGradient = true;

  /**
   * Sets if the dark mode is adapted to the app.
   * 
   * <p>It's not yet implemented.</p>
   */
  private boolean bsDarkMode = false;

  private String navbarBgColor;

  private String navbarBgColorPublic;

  private String navbarBgColorAccount;

  private String navbarBgColorAdmin;
  
  private String bodyBgColor = "#ffffff";

  private boolean showsMessagesLinkedToItemsAtTheTop = true;

  private boolean showsMessagesLinkedToItemsAtEachField = false;

  /**
   * Stores app-common modelAttributes which are not supported by the library.
   * 
   * <p>You can also set {@code @ModelAttribute} in {@code SystemCommonController}, 
   *     but it's easier to understand by integrating app-common modelAttributes here.</p>
   */
  private Map<String, Object> appCommonModelAttributeMap = new HashMap<>();


  public Map<String, Object> getAppCommonModelAttributeMap() {
    return appCommonModelAttributeMap;
  }

  /** 
   * Adds model attributes.
   */
  public void addAppCommonModelAttribute(String key, Object value) {
    this.appCommonModelAttributeMap.put(key, value);
  }

  public boolean getBsBgGradient() {
    return bsBgGradient;
  }

  public void setBsBgGradient(boolean bsBgGradient) {
    this.bsBgGradient = bsBgGradient;
  }

  public boolean getBsDarkMode() {
    return bsDarkMode;
  }

  public void setBsDarkMode(boolean bsDarkMode) {
    this.bsDarkMode = bsDarkMode;
  }

  public String getNavbarBgColor() {
    return navbarBgColor;
  }

  public void setNavbarBgColor(String navbarBgColor) {
    this.navbarBgColor = navbarBgColor;
  }

  public String getNavbarBgColorPublic() {
    return navbarBgColorPublic;
  }

  public void setNavbarBgColorPublic(String navbarBgColorPublic) {
    this.navbarBgColorPublic = navbarBgColorPublic;
  }

  public String getNavbarBgColorAccount() {
    return navbarBgColorAccount;
  }

  public void setNavbarBgColorAccount(String navbarBgColorAccount) {
    this.navbarBgColorAccount = navbarBgColorAccount;
  }

  public String getNavbarBgColorAdmin() {
    return navbarBgColorAdmin;
  }

  public void setNavbarBgColorAdmin(String navbarBgColorAdmin) {
    this.navbarBgColorAdmin = navbarBgColorAdmin;
  }

  public String getBodyBgColor() {
    return bodyBgColor;
  }

  public void setBodyBgColor(String bodyBgColor) {
    this.bodyBgColor = bodyBgColor;
  }

  public boolean isShowsMessagesLinkedToItemsAtTheTop() {
    return showsMessagesLinkedToItemsAtTheTop;
  }

  public void setShowsMessagesLinkedToItemsAtTheTop(boolean showsMessagesLinkedToItemsAtTheTop) {
    this.showsMessagesLinkedToItemsAtTheTop = showsMessagesLinkedToItemsAtTheTop;
  }

  public boolean isShowsMessagesLinkedToItemsAtEachField() {
    return showsMessagesLinkedToItemsAtEachField;
  }

  public void setShowsMessagesLinkedToItemsAtEachField(
      boolean showsMessagesLinkedToItemsAtEachField) {
    this.showsMessagesLinkedToItemsAtEachField = showsMessagesLinkedToItemsAtEachField;
  }

  /**
   * Adds all attributes to the argument model.
   * 
   * @param model model
   */
  public void addAllToModel(Model model) {
    model.addAttribute("bsBgGradient", bsBgGradient);
    model.addAttribute("bsDarkMode", bsDarkMode);
    model.addAttribute("navbarBgColor", navbarBgColor);
    model.addAttribute("navbarBgColorPublic", navbarBgColorPublic);
    model.addAttribute("navbarBgColorAccount", navbarBgColorAccount);
    model.addAttribute("navbarBgColorAdmin", navbarBgColorAdmin);
    model.addAttribute("bodyBgColor", bodyBgColor);
    model.addAttribute("showsMessagesLinkedToItemsAtTheTop", showsMessagesLinkedToItemsAtTheTop);
    model.addAttribute("showsMessagesLinkedToItemsAtEachField",
        showsMessagesLinkedToItemsAtEachField);
  }
}
