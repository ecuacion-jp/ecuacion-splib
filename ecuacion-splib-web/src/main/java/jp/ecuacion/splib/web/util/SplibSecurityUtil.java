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

/**
 * Provides utilities on spring security.
 */
public class SplibSecurityUtil {

  /**
   * Returns a new instance with no roles or authorities.
   */
  public RolesAndAuthoritiesBean getRolesAndAuthoritiesBean() {
    return createRolesAndAuthoritiesBean(new ArrayList<>());
  }

  /**
   * Returns RolesAndAuthoritiesBean from userDetails.
   */
  public RolesAndAuthoritiesBean getRolesAndAuthoritiesBean(UserDetails userDetails) {
    if (userDetails == null) {
      return getRolesAndAuthoritiesBean();

    } else {
      List<String> list = userDetails.getAuthorities().stream().map(e -> e.getAuthority()).toList();
      return createRolesAndAuthoritiesBean(list);
    }
  }

  /**
   * Returns bean from #authentication.principal.authorities in thymeleaf.
   */
  public RolesAndAuthoritiesBean getRolesAndAuthoritiesBean(String rolesOrAuthoritiesString) {
    rolesOrAuthoritiesString =
        rolesOrAuthoritiesString.replace("[", "").replace("]", "").replace(" ", "");

    return createRolesAndAuthoritiesBean(Arrays.asList(rolesOrAuthoritiesString.split(",")));
  }

  /**
   * Returns RolesAndAuthoritiesBean from string list of role / authority.
   */
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

  /**
   * Stores info on roles and authorities.
   */
  public static class RolesAndAuthoritiesBean {
    private List<String> roleList;
    private List<String> authorityList;

    /**
     * Constructs a new instance.
     * 
     * @param roleList roleList
     * @param authorityList authorityList
     */
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
