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
package jp.ecuacion.splib.web.bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdminScope")
class AdminScopeTest {

  @Test
  @DisplayName("unrestricted() has no values")
  void unrestricted() {
    assertThat(AdminScope.unrestricted().isUnrestricted()).isTrue();
    assertThatThrownBy(() -> AdminScope.unrestricted().getValues())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("of() holds the values")
  void of() {
    AdminScope scope = AdminScope.of(List.of(1L, 2L));

    assertThat(scope.isUnrestricted()).isFalse();
    assertThat(scope.getValues()).containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  @DisplayName("an empty collection is not unrestricted")
  void empty() {
    AdminScope scope = AdminScope.of(List.of());

    assertThat(scope.isUnrestricted()).isFalse();
    assertThat(scope.getValues()).isEmpty();
  }
}
