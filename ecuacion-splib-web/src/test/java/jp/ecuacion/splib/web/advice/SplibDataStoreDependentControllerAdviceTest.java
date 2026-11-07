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
package jp.ecuacion.splib.web.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.web.service.SplibDataStoreDependentControllerAdviceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ExtendedModelMap;

@DisplayName("SplibDataStoreDependentControllerAdvice")
class SplibDataStoreDependentControllerAdviceTest {

  private final List<String> executed = new ArrayList<>();

  private SplibDataStoreDependentControllerAdvice createAdvice(UserDetails loginUser) {
    SplibDataStoreDependentControllerAdviceService service =
        mock(SplibDataStoreDependentControllerAdviceService.class);
    SplibRecord rec = mock(SplibRecord.class);
    when(service.getAccAdmin(loginUser)).thenReturn(rec);
    when(service.getAccGeneral(loginUser)).thenReturn(rec);

    SplibDataStoreDependentControllerAdvice advice = new SplibDataStoreDependentControllerAdvice() {
      @Override
      protected void executeForAccountBelongingToGroup(SplibRecord acc) {
        executed.add("group");
      }

      @Override
      protected void executeForAdmin(UserDetails user) {
        executed.add("admin");
      }
    };
    advice.service = service;
    return advice;
  }

  private void execute(String role) {
    UserDetails loginUser = User.withUsername("user").password("").roles(role).build();
    createAdvice(loginUser).setAccountInfo(new ExtendedModelMap(), loginUser);
  }

  @Test
  @DisplayName("executeForAdmin is called for a role starting with ADMIN")
  void admin() {
    execute("ADMIN_LIMITED_DATA_ACCESS");
    assertThat(executed).containsExactly("admin");
  }

  @Test
  @DisplayName("executeForAccountBelongingToGroup is called for the other roles")
  void general() {
    execute("ACCOUNT");
    assertThat(executed).containsExactly("group");
  }

  @Test
  @DisplayName("getAdminScope throws an exception unless it's overridden")
  void getAdminScopeNotOverridden() {
    SplibDataStoreDependentControllerAdviceService service =
        new SplibDataStoreDependentControllerAdviceService() {
          @Override
          public SplibRecord getAccGeneral(UserDetails loginUser) {
            return mock(SplibRecord.class);
          }

          @Override
          public SplibRecord getAccAdmin(UserDetails loginUser) {
            return mock(SplibRecord.class);
          }

          @Override
          public Object getGroupId(SplibRecord loginAcc) {
            return 1L;
          }
        };

    assertThatThrownBy(() -> service.getAdminScope(mock(UserDetails.class)))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
