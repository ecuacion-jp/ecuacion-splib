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
package jp.ecuacion.splib.web.util.internal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Unit tests for {@link NotEmptyCheckUtil#isEmpty}.
 */
class NotEmptyCheckUtilTest {

  @Test
  void nullAndEmptyStringAreEmpty() {
    assertThat(NotEmptyCheckUtil.isEmpty(null)).isTrue();
    assertThat(NotEmptyCheckUtil.isEmpty("")).isTrue();
  }

  @Test
  void nonEmptyStringAndOtherObjectsAreNotEmpty() {
    assertThat(NotEmptyCheckUtil.isEmpty("a")).isFalse();
    assertThat(NotEmptyCheckUtil.isEmpty(Boolean.FALSE)).isFalse();
    assertThat(NotEmptyCheckUtil.isEmpty(new byte[0])).isFalse();
  }

  /** Browsers send a file part with {@code filename=""} when no file is chosen. */
  @Test
  void multipartFileWithoutChosenFileIsEmpty() {
    assertThat(NotEmptyCheckUtil.isEmpty(new MockMultipartFile("file", "", null, new byte[0])))
        .isTrue();
  }

  /** A chosen zero-byte file is an uploaded file, though {@code MultipartFile#isEmpty()}. */
  @Test
  void chosenZeroByteFileIsNotEmpty() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.txt", null, new byte[0]);

    assertThat(file.isEmpty()).isTrue();
    assertThat(NotEmptyCheckUtil.isEmpty(file)).isFalse();
  }

  @Test
  void chosenFileIsNotEmpty() {
    assertThat(NotEmptyCheckUtil.isEmpty(
        new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3}))).isFalse();
  }
}
