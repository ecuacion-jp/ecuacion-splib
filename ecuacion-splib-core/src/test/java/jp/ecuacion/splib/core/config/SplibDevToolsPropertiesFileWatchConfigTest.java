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
package jp.ecuacion.splib.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;

/**
 * Verifies the end-to-end effect of {@link SplibDevToolsPropertiesFileWatchConfig}: editing a
 * {@code *.properties} file under a directory-backed classpath entry is picked up by the
 * started {@link FileSystemWatcher} and results in {@link PropertiesFileUtil} returning the
 * new content on the next read, without an explicit {@link PropertiesFileUtil#clearCache()}
 * call from the test itself.
 *
 * <p>Writes to {@code constants_dyntest.properties} directly under {@code target/test-classes}
 * (located via this test class's own {@code CodeSource}, so the test is agnostic to the build
 * tool's exact layout) — a location genuinely on the classpath during {@code mvn test}, the same
 * kind of directory {@link SplibDevToolsPropertiesFileWatchConfig#propertiesFileWatcher()} finds
 * via {@code java.class.path} in a real dev run.</p>
 */
@DisplayName("SplibDevToolsPropertiesFileWatchConfig")
class SplibDevToolsPropertiesFileWatchConfigTest {

  private static final String KEY = "jp.ecuacion.splib.core.config.dyntest.key";

  private @Nullable File propertiesFile;
  private @Nullable FileSystemWatcher watcher;

  @SuppressWarnings("null")
  @AfterEach
  void cleanUp() {
    if (watcher != null) {
      watcher.stop();
    }
    if (propertiesFile != null) {
      propertiesFile.delete();
    }
    PropertiesFileUtil.clearCache();
  }

  @SuppressWarnings("null")
  @Test
  @DisplayName("editing a watched *.properties file clears the PropertiesFileUtil cache "
      + "so the next read reflects the new content")
  void propertiesFileWatcher_reloadsOnFileChange() throws IOException, URISyntaxException {
    PropertiesFileUtil.addResourceBundlePostfix("dyntest");

    File testClassesDir = new File(SplibDevToolsPropertiesFileWatchConfigTest.class
        .getProtectionDomain().getCodeSource().getLocation().toURI());
    propertiesFile = new File(testClassesDir, "constants_dyntest.properties");
    writeProperty(propertiesFile, "before");

    // Primes PropertiesFileUtil's ResourceBundle cache with the "before" content.
    assertThat(PropertiesFileUtil.getConstant(KEY)).isEqualTo("before");

    watcher = new SplibDevToolsPropertiesFileWatchConfig().propertiesFileWatcher();

    writeProperty(propertiesFile, "after");

    awaitConstantValue("after");
  }

  private void writeProperty(File file, String value) throws IOException {
    try (FileWriter writer = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
      writer.write(KEY + "=" + value + "\n");
    }
  }

  private void awaitConstantValue(String expected) {
    long deadline = System.currentTimeMillis() + 10_000;
    String actual;
    do {
      actual = PropertiesFileUtil.getConstant(KEY);
      if (expected.equals(actual)) {
        return;
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
      }
    } while (System.currentTimeMillis() < deadline);

    assertThat(actual).isEqualTo(expected);
  }
}
