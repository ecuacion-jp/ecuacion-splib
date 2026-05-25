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

import java.util.List;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.splib.core.bean.AuthorizationBean;
import org.jspecify.annotations.Nullable;

/**
 * Provides an abstract {@link SplibWebSecurityConfig} subclass
 * for applications that require no login.
 *
 * <p>Login-related methods ({@code getDefaultSuccessUrl}, {@code getLoginNeededPage})
 *     are sealed with {@code final} and return empty strings,
 *     as they are never called in a no-login application.</p>
 *
 * <p>Subclasses must implement {@link #getAccessDeniedPage()} only.
 *     {@link #getRoleInfo()} and {@link #getAuthorityInfo()} return {@code null} by default
 *     and may be overridden if needed.</p>
 *
 * <p>Usage: annotate the subclass with {@code @Configuration} and
 *     {@code @EnableWebSecurity}.</p>
 */
public abstract class SplibWebNoLoginSecurityConfig extends SplibWebSecurityConfig {

  /**
   * Constructs a new instance with no OAuth2 dependencies.
   */
  protected SplibWebNoLoginSecurityConfig() {
    super(null, null, null);
  }

  /**
   * Returns the home page path as the access-denied destination.
   *
   * <p>In a no-login application there is no login page to redirect to,
   *     so CSRF errors and other access-denied events are redirected to the home page
   *     defined by the {@code jp.ecuacion.splib.web.home-page} property instead.</p>
   *
   * <p>Override this method if the application needs a different destination.</p>
   *
   * @return the value of {@code jp.ecuacion.splib.web.home-page}
   */
  @Override
  protected String getAccessDeniedPage() {
    String homePage = PropertiesFileUtil.getApplication("jp.ecuacion.splib.web.home-page");
    return homePage.startsWith("/") ? homePage : "/" + homePage;
  }

  /**
   * Returns {@code false} to disable form login and logout.
   *
   * @return {@code false}
   */
  @Override
  protected final boolean isLoginEnabled() {
    return false;
  }

  /**
   * Unused — this application requires no login.
   *
   * @return empty string
   */
  @Override
  protected final String getDefaultSuccessUrl() {
    return "";
  }

  /**
   * Unused — this application requires no login.
   *
   * @return empty string
   */
  @Override
  protected final String getLoginNeededPage() {
    return "";
  }

  /**
   * Returns {@code null} by default (no role-based access control).
   *
   * <p>Override if the application needs role-based access control.</p>
   */
  @Override
  protected @Nullable List<AuthorizationBean> getRoleInfo() {
    return null;
  }

  /**
   * Returns {@code null} by default (no authority-based access control).
   *
   * <p>Override if the application needs authority-based access control.</p>
   */
  @Override
  protected @Nullable List<AuthorizationBean> getAuthorityInfo() {
    return null;
  }
}
