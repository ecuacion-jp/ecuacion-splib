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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * WebSecurityConfigのtemplate。 defaultSuccessUrlだけはシステム個別で設定したくなるため、abstractクラスとする。
 */
public abstract class SplibWebSecurityConfig {

  public static final String ACCOUNT_FULL_ACCESS = "ACCOUNT_FULL_ACCESS";

  protected PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /** ログイン成功時の遷移先を指定。 */
  protected abstract String getDefaultSuccessUrl();

  /**
   * ログインが必要なurlにアクセスした際の遷移先。login画面の場合と、それ以前の説明画面などの場合があるため個別設定可能とした。
   */
  protected abstract String getUrlWithLoginNeededPageAccessed();

  /**
   * ACCOUNT_FULL_ACCESSは指定の全てのpathに対して権限追加される仕組みになるようケアしているので、
   * ACCOUNT_FULL_ACCESSのことは気にせずに、それ以外のroleの設定のみをすれば良い。
   */
  protected abstract List<AuthorizationBean> getRoleInfo();

  protected abstract List<AuthorizationBean> getAuthorityInfo();

  /**
   * accessDeniedPageは、非ログイン時にpermitAllでないパスにアクセスした場合に加え、csrf tokenのエラーの場合も発生。
   * 後者は、イコールpermitAllのpathにアクセスした場合に毎度発生することになるため、特にログインのないアプリでは設定変更が必要。
   * ただし、abstractにはせず任意での変更項目としておく。
   */
  protected String getAccessDeniedPage() {
    return "/public/login/page?accessDenied";
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return passwordEncoder;
  }

  @Bean
  MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
    return new MvcRequestMatcher.Builder(introspector);
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc)
      throws Exception {

    http.httpBasic(basic -> basic.disable());

    http.formLogin(login -> login.loginPage(getUrlWithLoginNeededPageAccessed())
        .loginProcessingUrl("/public/login/action").usernameParameter("login.username")
        .passwordParameter("login.password").defaultSuccessUrl(getDefaultSuccessUrl(), true)
        .failureUrl("/public/login/page?error"));

    http.authorizeHttpRequests(
        requests -> requests.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
            .permitAll().requestMatchers(mvc.pattern("/public/**")).permitAll()
            .requestMatchers(mvc.pattern("/ecuacion/public/**")).permitAll());

    // 管理者など、ログイン後の/account配下の全画面が閲覧可能としたいroleは、ACCOUNT_FULL_ACCESSのroleを設定すればOK。
    List<AuthorizationBean> roleList =
        getRoleInfo() == null ? new ArrayList<>() : new ArrayList<>(getRoleInfo());
    roleList.add(new AuthorizationBean("/account/**", ACCOUNT_FULL_ACCESS));
    for (AuthorizationBean bean : roleList) {
      // 画面別の細かい設定に対して、ACCOUNT_FULL_ACCESSも設定しておかないとその画面にACCOUNT_FULL_ACCESSでアクセス不可となる。
      // 本来は個々のApp側できちんとやるべき話かもしれないが、わかりにくい仕組みなのでsplib側でACCOUNT_FULL_ACCESSを補完する機能を保持しておく。
      http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
          .hasAnyRole(bean.addAndGetRolesOrAuthorities(ACCOUNT_FULL_ACCESS)));
    }

    // roleとauthorityを組み合わせたテストはできていないので、その実施時に適切に動かなかった場合は要修正・・・
    if (getAuthorityInfo() != null) {
      for (AuthorizationBean bean : getAuthorityInfo()) {
        http.authorizeHttpRequests(requests -> requests.requestMatchers(bean.getRequestMatchers())
            .hasAnyAuthority(bean.getRolesOrAuthorities()));
      }
    }

    http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());

    http.logout(logout -> logout.logoutUrl("/public/logout")
        .logoutSuccessUrl("/public/login/page?logoutDone"));

    http.exceptionHandling(handling -> handling.accessDeniedPage(getAccessDeniedPage()));

    return http.build();
  }
}
