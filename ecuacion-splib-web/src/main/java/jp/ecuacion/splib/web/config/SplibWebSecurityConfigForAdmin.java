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
package jp.ecuacion.splib.web.config;


import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 *  Provides the abstract SecurityConfig class for admin.
 * 
 * <p>Since using this class is not mandatory in the library, 
 *     it's abstract and It has no annotations to be recognized as it.
 *     If you want to use this, create a new class 
 *     which extends it and put class annotations on the new class: 
 *     {@code Configuration} and {@code EnableWebSecurity}.</p>
 */
public abstract class SplibWebSecurityConfigForAdmin {

  /**
   * Defines the string for the role "ACCOUNT_FULL_ACCESS".
   */
  public static final String ADMIN_FULL_ACCESS = "ADMIN_FULL_ACCESS";

  /**
   * Returns the url when the login procedure successfully ended.
   */
  protected abstract String getDefaultSuccessUrl();

  /**
   * Returns the url when the login needed page when there is no logged in account in the session.
   */
  protected abstract String getLoginNeededPage();

  /**
   * Returns the role list of {@code AuthorizationBean}.
   * 
   * <p>There's a reserved role: {@code ACCOUNT_FULL_ACCESS}. 
   *     This offers full access to /account/** 
   *     so it's easily used for admin user or power user.</p>
   *     
   * @return the role list of AuthorizationBean
   */
  protected abstract List<AuthorizationBean> getRoleInfo();

  /**
   * Returns the authority list of {@code AuthorizationBean}.
   * 
   * @return the authority list of AuthorizationBean
   */
  protected abstract List<AuthorizationBean> getAuthorityInfo();

  /**
   * Returns the url when the access denied page is accessed.
   * 
   * <p>This happens in the case of non-exist url access and csrf token error.</p>
   */
  protected abstract String getAccessDeniedPage();

  /**
   * Adds security settings to the {@code HttpSecurity} object. 
   * 
   * <p>Since {@code @Order(11)} is added to the method, 
   *     the priority is higher than {@code SplibWebSecurityConfig#filterChain},
   *     and lower than {@code jp.ecuacion.splib.rest.config.SplibRestSecurityConfig}.</p>
   */
  @Order(11)
  @Bean
  SecurityFilterChain filterChainForAdmin(HttpSecurity http,
      HandlerMappingIntrospector introspector) throws Exception {
    MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

    http.securityMatcher("/public/admin*/**", "/admin/**");

    http.httpBasic(basic -> basic.disable());

    http.formLogin(login -> login.loginPage(getLoginNeededPage())
        .loginProcessingUrl("/public/adminLogin/action").usernameParameter("adminLogin.username")
        .passwordParameter("adminLogin.password").defaultSuccessUrl(getDefaultSuccessUrl(), true)
        .failureUrl("/public/adminLogin/page?error"));

    http.authorizeHttpRequests(
        requests -> requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
            .permitAll().requestMatchers(mvc.pattern("/public/admin*/**")).permitAll());

    // 管理者など、ログイン後の/admin配下の全画面が閲覧可能としたいroleは、ADMIN_FULL_ACCESSのroleを設定すればOK。
    List<AuthorizationBean> roleList = getRoleInfo() == null ? new ArrayList<>() : getRoleInfo();
    roleList.add(new AuthorizationBean("/admin/**", ADMIN_FULL_ACCESS));
    for (AuthorizationBean bean : roleList) {
      // 画面別の細かい設定に対して、ADMIN_FULL_ACCESSも設定しておかないとその画面にADMIN_FULL_ACCESSでアクセス不可となる。
      // 本来は個々のApp側できちんとやるべき話かもしれないが、わかりにくい仕組みなのでsplib側でADMIN_FULL_ACCESSを補完する機能を保持しておく。
      http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
          .hasAnyRole(bean.addAndGetRolesOrAuthorities(ADMIN_FULL_ACCESS)));
    }

    // roleとauthorityを組み合わせたテストはできていないので、その実施時に適切に動かなかった場合は要修正・・・
    if (getAuthorityInfo() != null) {
      for (AuthorizationBean bean : getAuthorityInfo()) {
        http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
            .hasAnyAuthority(bean.getRolesOrAuthorities()));
      }
    }

    http.logout(logout -> logout.logoutUrl("/public/adminLogout")
        .logoutSuccessUrl("/public/adminLogin/page?logoutDone"));

    http.exceptionHandling(handling -> handling.accessDeniedPage(getAccessDeniedPage()));

    return http.build();
  }
}
