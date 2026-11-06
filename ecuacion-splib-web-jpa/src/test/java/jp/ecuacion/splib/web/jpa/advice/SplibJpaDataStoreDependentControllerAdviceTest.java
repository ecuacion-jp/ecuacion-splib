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
package jp.ecuacion.splib.web.jpa.advice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import jp.ecuacion.splib.jpa.util.SplibJpaFilterUtil;
import jp.ecuacion.splib.web.bean.AdminScope;
import jp.ecuacion.splib.web.service.SplibDataStoreDependentControllerAdviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

@DisplayName("SplibJpaDataStoreDependentControllerAdvice#executeForAdmin")
class SplibJpaDataStoreDependentControllerAdviceTest {

  private final UserDetails loginUser = mock(UserDetails.class);

  @SuppressWarnings("null")
  private SplibDataStoreDependentControllerAdviceService service;
  @SuppressWarnings("null")
  private SplibJpaFilterUtil filterUtil;
  @SuppressWarnings("null")
  private SplibJpaDataStoreDependentControllerAdvice advice;

  @BeforeEach
  void setUp() {
    service = mock(SplibDataStoreDependentControllerAdviceService.class);
    filterUtil = mock(SplibJpaFilterUtil.class);
    advice = new SplibJpaDataStoreDependentControllerAdvice();
    advice.service = service;
    advice.filterUtil = filterUtil;
  }

  @Test
  @DisplayName("doesn't call getAdminScope when the app doesn't use the feature")
  void featureNotUsed() {
    when(filterUtil.usesAdminScopeFeature()).thenReturn(false);

    advice.executeForAdmin(loginUser);

    verify(service, never()).getAdminScope(any());
    verify(filterUtil, never()).enableAdminScopeFilter(any());
  }

  @Test
  @DisplayName("doesn't enable the filter when the scope is unrestricted")
  void unrestricted() {
    when(filterUtil.usesAdminScopeFeature()).thenReturn(true);
    when(service.getAdminScope(loginUser)).thenReturn(AdminScope.unrestricted());

    advice.executeForAdmin(loginUser);

    verify(filterUtil, never()).enableAdminScopeFilter(any());
  }

  @Test
  @DisplayName("enables the filter with the values")
  void restricted() {
    when(filterUtil.usesAdminScopeFeature()).thenReturn(true);
    when(service.getAdminScope(loginUser)).thenReturn(AdminScope.of(List.of(1L)));

    advice.executeForAdmin(loginUser);

    verify(filterUtil).enableAdminScopeFilter(Set.of(1L));
  }

  @Test
  @DisplayName("enables the filter even when the values are empty")
  void empty() {
    when(filterUtil.usesAdminScopeFeature()).thenReturn(true);
    when(service.getAdminScope(loginUser)).thenReturn(AdminScope.of(List.of()));

    advice.executeForAdmin(loginUser);

    verify(filterUtil).enableAdminScopeFilter(Set.of());
  }
}
