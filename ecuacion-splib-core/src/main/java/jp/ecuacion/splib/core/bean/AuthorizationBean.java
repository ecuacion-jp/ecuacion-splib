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
 * Stores url pattern for accessibility for roles and authorities.
 * 
 * <p>It's for web-related modules only, but is also used by multiple modules (web, rest)
 *     so it's placed in {@code ecuacion-lib-core}. 
 */
public class AuthorizationBean {
  private String[] requestMatchers;
  private String[] rolesOrAuthorities;

  /**
   * Constructs a new instance.
   */
  public AuthorizationBean() {

  }

  /**
   * Constructs a new instance.
   * 
   * @param requestMatchers requestMatchers
   * @param roleOrAuthority roleOrAuthority
   */
  public AuthorizationBean(String requestMatchers, String roleOrAuthority) {
    this.requestMatchers = new String[] {requestMatchers};
    this.rolesOrAuthorities = new String[] {roleOrAuthority};
  }

  /**
   * Constructs a new instance.
   * 
   * @param requestMatchers requestMatchers
   * @param roleOrAuthority roleOrAuthority
   */
  public AuthorizationBean(String[] requestMatchers, String roleOrAuthority) {
    this.rolesOrAuthorities = new String[] {roleOrAuthority};
    this.requestMatchers = requestMatchers;
  }

  /**
   * Constructs a new instance.
   * 
   * @param requestMatchers requestMatchers
   * @param roleOrAuthority roleOrAuthority
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
   * Adds the argument role or authority and returns the array.
   * 
   * @param addedRoleOrAuthority addedRoleOrAuthority
   * @return String[]
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
