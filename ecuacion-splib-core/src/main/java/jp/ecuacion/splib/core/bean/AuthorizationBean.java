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
package jp.ecuacion.splib.core.bean;

/** 
 * urlパターンと、そのurlパターンに合致する先にアクセスするのに必要なroleないしAuthorityを設定するためのbean。
 * web関連で使用するものだが、webでもrestでも使用するためcoreにて保持。 
 */
public class AuthorizationBean {
  private String[] requestMatchers;
  private String[] rolesOrAuthorities;

  /**
   * 
   */
  public AuthorizationBean() {

  }

  /**
   * 
   * @param requestMatchers
   * @param roleOrAuthority
   */
  public AuthorizationBean(String requestMatchers, String roleOrAuthority) {
    this.requestMatchers = new String[] {requestMatchers};
    this.rolesOrAuthorities = new String[] {roleOrAuthority};
  }

  /**
   * 
   * @param requestMatchers
   * @param roleOrAuthority
   */
  public AuthorizationBean(String[] requestMatchers, String roleOrAuthority) {
    this.rolesOrAuthorities = new String[] {roleOrAuthority};
    this.requestMatchers = requestMatchers;
  }

  /**
   * 
   * @param requestMatchers
   * @param roleOrAuthority
   */
  public AuthorizationBean(String[] requestMatchers, String[] roleOrAuthority) {
    this.rolesOrAuthorities = roleOrAuthority;
    this.requestMatchers = requestMatchers;
  }

  public String[] getRequestMatchers() {
    return requestMatchers;
  }

  public void setRequestMatchers(String[] requestMatchers) {
    this.requestMatchers = requestMatchers;
  }

  public String[] getRolesOrAuthorities() {
    return rolesOrAuthorities;
  }

  public void setRolesOrAuthorities(String[] roleOrAuthorities) {
    this.rolesOrAuthorities = roleOrAuthorities;
  }

  /**
   * 
   * @param addedRoleOrAuthority
   * @return
   */
  public String[] addAndGetRolesOrAuthorities(String addedRoleOrAuthority) {
    String[] rtnArr = new String[rolesOrAuthorities.length + 1];
    for (int i = 0; i < rolesOrAuthorities.length; i++) {
      // 追加しようとするroleOrAuthorityが既に設定されている場合は追加する必要がないのでそのまま終了
      if (rolesOrAuthorities[i].equals(addedRoleOrAuthority)) {
        return rolesOrAuthorities;
      }
      
      rtnArr[i] = rolesOrAuthorities[i];
    }
    
    rtnArr[rolesOrAuthorities.length] = addedRoleOrAuthority;
    return rtnArr;
  }
}