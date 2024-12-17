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
package jp.ecuacion.splib.web.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;

public class SplibSecurityUtil {
  
  /** 空のbeanを生成。role、authorityが存在しない状態で生成される。 */
  public RolesAndAuthoritiesBean getRolesAndAuthoritiesBean() {
    return createRolesAndAuthoritiesBean(new ArrayList<>());
  }
  
  /** UserDetailsからbeanを生成。 */
  public RolesAndAuthoritiesBean getRolesAndAuthoritiesBean(UserDetails userDetails) {
    if (userDetails == null) {
      return getRolesAndAuthoritiesBean();

    } else {
      List<String> list = userDetails.getAuthorities().stream().map(e -> e.getAuthority()).toList();
      return createRolesAndAuthoritiesBean(list);
    }
  }

  /** thymeleafで#authentication.principal.authorities にて取得した文字列からbeanを生成。 */
  public RolesAndAuthoritiesBean getRolesAndAuthoritiesBean(String rolesOrAuthoritiesString) {
    rolesOrAuthoritiesString =
        rolesOrAuthoritiesString.replace("[", "").replace("]", "").replace(" ", "");
    
    return createRolesAndAuthoritiesBean(Arrays.asList(rolesOrAuthoritiesString.split(",")));
  }
  
  /**role / authority を、stringのlistの形にしたものを引数に渡してRolesAndAuthoritiesBeanを返す。 */
  private RolesAndAuthoritiesBean createRolesAndAuthoritiesBean(List<String> list) {
    List<String> roleList = new ArrayList<>();
    List<String> authorityList = new ArrayList<>();
    
    for (String authorityOrRole : list) {
      if (authorityOrRole.startsWith("ROLE_")) {
        roleList.add(authorityOrRole.substring(5));

      } else {
        authorityList.add(authorityOrRole);
      }
    }

    return new RolesAndAuthoritiesBean(roleList, authorityList);
  }

  public static class RolesAndAuthoritiesBean {
    private List<String> roleList;
    private List<String> authorityList;

    public RolesAndAuthoritiesBean(List<String> roleList, List<String> authorityList) {
      this.roleList = roleList;
      this.authorityList = authorityList;
    }

    public List<String> getRoleList() {
      return roleList;
    }

    public List<String> getAuthorityList() {
      return authorityList;
    }
  }
}
