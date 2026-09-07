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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.devtools.filewatch.FileSystemWatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clears the {@link PropertiesFileUtil} cache automatically whenever a file changes under a
 * directory-backed classpath entry, for local development convenience.
 *
 * <p>{@link PropertiesFileUtil} reads {@code *.properties} files through
 * {@link java.util.ResourceBundle}, which caches forever until
 * {@link PropertiesFileUtil#clearCache()} is called (ecuacion-splib-rest exposes that as an
 * authenticated REST endpoint for production use). Without this class, editing
 * {@code messages.properties} (or {@code item_names}, {@code constants}, {@code enum_names},
 * ...) during development requires either an application restart or a manual call to that
 * endpoint to see the change take effect.</p>
 *
 * <p>Reacts to any file change (not only {@code *.properties}), including {@code .class} files
 * recompiling — {@link FileSystemWatcher#setTriggerFilter} looked like the right tool to narrow
 * this down, but (verified empirically) its filter selects which changed files are excluded
 * from the reported change set, the opposite of an allow-list; getting that inverted is an easy
 * way to silently stop detecting the very changes this class exists to react to. Since
 * {@link PropertiesFileUtil#clearCache()} is cheap, the simpler and safer choice is to just not
 * filter at all.</p>
 *
 * <p>Only active when Spring Boot DevTools' {@link FileSystemWatcher} is on the classpath — a
 * dependency that is conventionally excluded from production artifacts (the Spring Boot Maven
 * Plugin excludes it from the repackaged executable jar by default), so this never runs in
 * production. It can also be turned off explicitly for local development via
 * {@code jp.ecuacion.splib.core.dev.properties-file-watch.enabled=false}.</p>
 *
 * <p>This intentionally does not attempt a real reload of {@code application.properties}
 * values or a Spring context restart; it only clears {@link PropertiesFileUtil}'s cache so the
 * next read re-parses the file from disk. Automatic reloading of bundled properties files in
 * production is explicitly out of scope for this library (see
 * {@code SplibCoreConfig#validateUnsupportedSettings} rejecting
 * {@code spring.messages.cache-duration}); this class exists only for the local, dev-time
 * convenience of not having to restart or call the endpoint by hand.</p>
 */
@Configuration
@ConditionalOnClass(FileSystemWatcher.class)
@ConditionalOnProperty(prefix = "jp.ecuacion.splib.core.dev.properties-file-watch",
    name = "enabled", matchIfMissing = true)
public class SplibDevToolsPropertiesFileWatchConfig {

  private final DetailLogger detailLog = new DetailLogger(this);

  /**
   * Creates and starts a {@link FileSystemWatcher} over the directory-backed classpath entries
   * (typically {@code target/classes} in a Maven dev run).
   *
   * @return the started watcher; stopped automatically on context shutdown
   */
  @Bean(destroyMethod = "stop")
  FileSystemWatcher propertiesFileWatcher() {
    FileSystemWatcher watcher = new FileSystemWatcher();
    classpathDirectories().forEach(watcher::addSourceDirectory);

    watcher.addListener(changeSet -> {
      PropertiesFileUtil.clearCache();
      detailLog.info("PropertiesFileUtil cache was cleared automatically: a file change was "
          + "detected under a watched classpath directory (dev only, via Spring Boot DevTools).");
    });

    watcher.start();
    return watcher;
  }

  /**
   * Returns the directory-backed entries of {@code java.class.path}.
   *
   * <p>A packaged jar entry's timestamp never changes at runtime, so only directory-backed
   * entries (loose {@code .class}/{@code .properties} files, as produced by a Maven/Gradle dev
   * build) are worth watching.</p>
   */
  private List<File> classpathDirectories() {
    List<File> dirs = new ArrayList<>();
    for (String entry : Objects.requireNonNull(System.getProperty("java.class.path", ""))
        .split(File.pathSeparator, -1)) {
      File file = new File(entry);
      if (file.isDirectory()) {
        dirs.add(file);
      }
    }
    return dirs;
  }
}
