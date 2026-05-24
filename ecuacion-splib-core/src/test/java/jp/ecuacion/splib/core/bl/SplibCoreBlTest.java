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
package jp.ecuacion.splib.core.bl;

import java.util.List;
import java.util.Optional;
import jp.ecuacion.lib.core.exception.ViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SplibCoreBl")
class SplibCoreBlTest {

  private final SplibCoreBl bl = new SplibCoreBl();

  @Nested
  @DisplayName("internalChildExistenceCheck(List)")
  class InternalChildExistenceCheckWithList {

    @Test
    @DisplayName("Does not throw an exception when the list is empty")
    void noExceptionWhenListIsEmpty() {
      assertThatCode(() -> bl.internalChildExistenceCheck(List.of(), "someEntity"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Throws ViolationException when the list is not empty")
    void throwsExceptionWhenListIsNotEmpty() {
      assertThatThrownBy(() -> bl.internalChildExistenceCheck(List.of("item"), "someEntity"))
          .isInstanceOf(ViolationException.class);
    }
  }

  @Nested
  @DisplayName("internalChildExistenceCheck(Optional)")
  class InternalChildExistenceCheckWithOptional {

    @Test
    @DisplayName("Does not throw an exception when the Optional is empty")
    void noExceptionWhenOptionalIsEmpty() {
      assertThatCode(() -> bl.internalChildExistenceCheck(Optional.empty(), "someEntity"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Throws ViolationException when the Optional has a value")
    void throwsExceptionWhenOptionalIsPresent() {
      assertThatThrownBy(() -> bl.internalChildExistenceCheck(Optional.of("item"), "someEntity"))
          .isInstanceOf(ViolationException.class);
    }
  }
}
