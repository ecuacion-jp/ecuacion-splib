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
 * WebSecurityConfigのtemplate。 本templateが使用可能な能合と不可能な場合があるため、abstractクラスとし、EnableWebSecurity
 * などの必要なannotationは子クラス側でつける形とする。
 * 
 * <p>
 * adminを使用する場合は本クラスを使用することになるが、その場合はSplibWebSecurityConfigも使用される前提とし、
 * かつそのinstanceより本instanceを先に実行する前提のため、Order(2) annotationをつけている。
 * Orderの一桁台は、/api/で使用することを想定し11としている。apiを使用しない状態で本クラスを使用しても問題はない。
 * </p>
 */
public abstract class SplibWebSecurityConfigForAdmin {

  public static final String ADMIN_FULL_ACCESS = "ADMIN_FULL_ACCESS";

  /** ログイン成功時の遷移先を指定。 */
  protected abstract String getDefaultSuccessUrl();

  /**
   * ログインが必要なurlにアクセスした際の遷移先。login画面の場合と、それ以前の説明画面などの場合があるため個別設定可能とした。
   */
  protected abstract String getUrlWithLoginNeededPageAccessed();

  /**
   * ADMIN_FULL_ACCESSは指定の全てのpathに対して権限追加される仕組みになるようケアしているので、
   * ADMIN_FULL_ACCESSのことは気にせずに、それ以外のroleの設定のみをすれば良い。
   */
  protected abstract List<AuthorizationBean> getRoleInfo();

  protected abstract List<AuthorizationBean> getAuthorityInfo();

  /**
   * accessDeniedPageは、非ログイン時にpermitAllでないパスにアクセスした場合に加え、csrf tokenのエラーの場合も発生。
   * 後者は、イコールpermitAllのpathにアクセスした場合に毎度発生することになるため、特にログインのないアプリでは設定変更が必要。
   * ただし、abstractにはせず任意での変更項目としておく。
   */
  protected String getAccessDeniedPage() {
    return "/public/adminLogin/page?accessDenied";
  }

  @Order(11)
  @Bean
  SecurityFilterChain filterChainForAdmin(HttpSecurity http,
      HandlerMappingIntrospector introspector) throws Exception {
    MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

    http.securityMatcher("/public/admin*/**", "/admin/**");

    http.httpBasic(basic -> basic.disable());

    http.formLogin(login -> login.loginPage(getUrlWithLoginNeededPageAccessed())
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
