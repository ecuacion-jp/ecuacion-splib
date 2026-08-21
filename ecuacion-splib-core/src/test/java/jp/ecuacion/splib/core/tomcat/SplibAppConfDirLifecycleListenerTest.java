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
package jp.ecuacion.splib.core.tomcat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Two layers of coverage: fast unit tests that invoke {@link
 * SplibAppConfDirLifecycleListener#lifecycleEvent} directly against a bare {@link
 * StandardContext} (exercising the listener's own path-resolution / create-or-skip / mounting
 * logic without paying for a full server startup), plus one slower end-to-end test that actually
 * starts an embedded {@link Tomcat} — the only way to genuinely prove the central claim: that
 * mounting a directory this way, instead of via a static {@code <PreResources>} entry, avoids
 * the {@code IllegalArgumentException} Tomcat itself throws for a missing {@code PreResources}
 * base directory (see the class javadoc's lifecycle-timing note for why the timing works).
 */
@DisplayName("SplibAppConfDirLifecycleListener")
class SplibAppConfDirLifecycleListenerTest {

  private static final String PROP_NAME = "splibAppConfDirLifecycleListenerTest.customPath";

  @AfterEach
  void clearProperty() {
    System.clearProperty(PROP_NAME);
  }

  private static LifecycleEvent beforeStartEvent(StandardContext context) {
    return new LifecycleEvent(context, Lifecycle.BEFORE_START_EVENT, null);
  }

  @Test
  @DisplayName("lifecycleEvent: an event other than BEFORE_START_EVENT is ignored")
  void lifecycleEvent_ignoresOtherEvents(@TempDir Path tempDir) {
    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    listener.setDefaultPath(tempDir.resolve("unused").toString());

    StandardContext context = new StandardContext();
    listener.lifecycleEvent(new LifecycleEvent(context, Lifecycle.AFTER_START_EVENT, null));

    assertThat(context.getResources()).isNull();
  }

  @Test
  @DisplayName("lifecycleEvent: throws when neither defaultPath nor customPathPropertyName is "
      + "set")
  void lifecycleEvent_throwsWhenNeitherDefaultPathNorCustomPathPropertyNameSet() {
    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    StandardContext context = new StandardContext();

    assertThatThrownBy(() -> listener.lifecycleEvent(beforeStartEvent(context)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("defaultPath")
        .hasMessageContaining("customPathPropertyName");
  }

  @Test
  @DisplayName("lifecycleEvent: throws when createDefaultPathIfMissing=true but defaultPath is "
      + "unset")
  void lifecycleEvent_throwsWhenCreateDefaultPathIfMissingTrueButDefaultPathUnset() {
    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    listener.setCreateDefaultPathIfMissing(true);

    StandardContext context = new StandardContext();

    assertThatThrownBy(() -> listener.lifecycleEvent(beforeStartEvent(context)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("createDefaultPathIfMissing")
        .hasMessageContaining("defaultPath");
  }

  @Test
  @DisplayName("lifecycleEvent: defaultPath unset, customPathPropertyName set but not present "
      + "as a system property -> nothing mounted, no exception")
  void lifecycleEvent_defaultPathUnsetAndCustomPathPropertyNotSet_mountsNothing() {
    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    // defaultPath intentionally left unset, and PROP_NAME is not set as a system property.

    StandardContext context = new StandardContext();
    listener.lifecycleEvent(beforeStartEvent(context));

    assertThat(context.getResources()).isNull();
  }

  @Test
  @DisplayName("lifecycleEvent: defaultPath unset, custom path system property set -> mounted, "
      + "created")
  void lifecycleEvent_defaultPathUnsetAndCustomPathPropertySet_createsAndMounts(
      @TempDir Path tempDir) {
    Path customDir = tempDir.resolve("custom-app-conf");
    System.setProperty(PROP_NAME, customDir.toString());

    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    // defaultPath intentionally left unset.

    StandardContext context = new StandardContext();
    listener.lifecycleEvent(beforeStartEvent(context));

    assertThat(Files.isDirectory(customDir)).isTrue();
    assertThat(context.getResources()).isNotNull();
  }

  @Test
  @DisplayName("lifecycleEvent: customPathPropertyName left unset -> defaultPath is used "
      + "directly, no exception")
  void lifecycleEvent_customPathPropertyNameUnset_usesDefaultPath(@TempDir Path tempDir) {
    Path defaultDir = tempDir.resolve("shared-app-conf");
    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    // customPathPropertyName intentionally left unset.
    listener.setDefaultPath(defaultDir.toString());
    listener.setCreateDefaultPathIfMissing(true);

    StandardContext context = new StandardContext();
    listener.lifecycleEvent(beforeStartEvent(context));

    assertThat(Files.isDirectory(defaultDir)).isTrue();
    assertThat(context.getResources()).isNotNull();
  }

  @Test
  @DisplayName("lifecycleEvent: default path, createDefaultPathIfMissing=false, "
      + "directory missing -> skipped without error, nothing created or mounted")
  void lifecycleEvent_defaultPathMissingAndCreateFalse_skips(@TempDir Path tempDir) {
    Path missing = tempDir.resolve("app-conf-child");
    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    listener.setDefaultPath(missing.toString());
    // createDefaultPathIfMissing left at its default (false).

    StandardContext context = new StandardContext();
    listener.lifecycleEvent(beforeStartEvent(context));

    assertThat(Files.exists(missing)).isFalse();
    assertThat(context.getResources()).isNull();
  }

  @Test
  @DisplayName("lifecycleEvent: default path, createDefaultPathIfMissing=true, "
      + "directory missing -> created and mounted")
  void lifecycleEvent_defaultPathMissingAndCreateTrue_createsAndMounts(@TempDir Path tempDir) {
    Path missing = tempDir.resolve("app-conf-child");
    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    listener.setDefaultPath(missing.toString());
    listener.setCreateDefaultPathIfMissing(true);

    StandardContext context = new StandardContext();
    listener.lifecycleEvent(beforeStartEvent(context));

    assertThat(Files.isDirectory(missing)).isTrue();
    WebResourceRoot resources = context.getResources();
    assertThat(resources).isNotNull();
    assertThat(resources.getPreResources()).hasSize(1);
  }

  @Test
  @DisplayName("lifecycleEvent: custom path system property set -> overrides defaultPath, "
      + "created and mounted regardless of createDefaultPathIfMissing")
  void lifecycleEvent_customPathPropertySet_overridesAndCreates(@TempDir Path tempDir) {
    Path customDir = tempDir.resolve("custom-app-conf");
    Path defaultDir = tempDir.resolve("default-app-conf");
    System.setProperty(PROP_NAME, customDir.toString());

    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    listener.setDefaultPath(defaultDir.toString());
    // createDefaultPathIfMissing left false: irrelevant once a custom path is given.

    StandardContext context = new StandardContext();
    listener.lifecycleEvent(beforeStartEvent(context));

    assertThat(Files.isDirectory(customDir)).isTrue();
    assertThat(Files.exists(defaultDir)).isFalse();
  }

  @Test
  @DisplayName("lifecycleEvent: directory already exists -> mounted as-is, no error")
  void lifecycleEvent_directoryAlreadyExists_mountsWithoutCreating(@TempDir Path tempDir)
      throws Exception {
    Path existing = tempDir.resolve("already-there");
    Files.createDirectories(existing);

    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    listener.setDefaultPath(existing.toString());

    StandardContext context = new StandardContext();
    listener.lifecycleEvent(beforeStartEvent(context));

    assertThat(context.getResources()).isNotNull();
    assertThat(context.getResources().getPreResources()).hasSize(1);
  }

  /**
   * The end-to-end proof: starts a real embedded Tomcat with a context whose {@code
   * app-conf}-equivalent directory does not exist beforehand. Without this listener (i.e. with a
   * static {@code <PreResources>} pointed at the same missing directory), Tomcat would throw
   * {@code IllegalArgumentException} and the context would fail to start. With it, {@code
   * tomcat.start()} succeeds, the directory is created, and a marker file placed in it is
   * actually visible through the mounted resource root — proving this isn't just "no exception"
   * but a genuinely working classpath mount.
   */
  @Test
  @DisplayName("End-to-end: embedded Tomcat starts successfully with a missing app-conf "
      + "directory, which gets created and genuinely mounted onto the classpath")
  void endToEnd_embeddedTomcatStartsAndMountsCreatedDirectory(@TempDir Path tempDir)
      throws Exception {
    Path appConfDir = tempDir.resolve("app-conf").resolve("my-app");
    Path docBase = tempDir.resolve("docbase");
    Files.createDirectories(docBase);
    assertThat(Files.exists(appConfDir)).isFalse();

    Tomcat tomcat = new Tomcat();
    tomcat.setPort(0);
    tomcat.setBaseDir(tempDir.resolve("catalina-base").toString());
    tomcat.getHost().setAppBase(".");

    Context context = tomcat.addContext("", docBase.toAbsolutePath().toString());

    SplibAppConfDirLifecycleListener listener = new SplibAppConfDirLifecycleListener();
    listener.setCustomPathPropertyName(PROP_NAME);
    listener.setDefaultPath(appConfDir.toString());
    listener.setCreateDefaultPathIfMissing(true);
    context.addLifecycleListener(listener);

    try {
      tomcat.start();

      assertThat(Files.isDirectory(appConfDir)).isTrue();

      // Written only after the mount is live, to prove the mounted resource root reflects the
      // real directory rather than some snapshot taken before it existed.
      Files.writeString(appConfDir.resolve("marker.txt"), "hello");
      WebResource resource = context.getResources().getResource("/WEB-INF/classes/marker.txt");
      assertThat(resource.exists()).isTrue();
      assertThat(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
          .isEqualTo("hello");
    } finally {
      tomcat.stop();
      tomcat.destroy();
    }
  }

  /**
   * Mirrors {@code ecuacion-tool-command-api}'s real {@code context.xml}: one {@code <Listener>}
   * for an app-specific directory (declared first) and a second, simpler one (no {@link
   * #setCustomPathPropertyName}) for a directory shared across co-located apps (declared
   * second). A file present in both must resolve from the app-specific one — the same
   * first-declared-wins priority two static {@code <PreResources>} entries would have.
   */
  @Test
  @DisplayName("End-to-end: with two Listeners for an app-specific dir and a shared dir, "
      + "a file present in both resolves from the app-specific (first-declared) one")
  void endToEnd_twoListeners_appSpecificDirTakesPriorityOverSharedDir(@TempDir Path tempDir)
      throws Exception {
    Path appSpecificDir = tempDir.resolve("app-conf").resolve("my-app");
    Path sharedDir = tempDir.resolve("app-conf");
    Path docBase = tempDir.resolve("docbase");
    Files.createDirectories(docBase);

    Tomcat tomcat = new Tomcat();
    tomcat.setPort(0);
    tomcat.setBaseDir(tempDir.resolve("catalina-base").toString());
    tomcat.getHost().setAppBase(".");

    Context context = tomcat.addContext("", docBase.toAbsolutePath().toString());

    SplibAppConfDirLifecycleListener appSpecificListener = new SplibAppConfDirLifecycleListener();
    appSpecificListener.setCustomPathPropertyName(PROP_NAME);
    appSpecificListener.setDefaultPath(appSpecificDir.toString());
    appSpecificListener.setCreateDefaultPathIfMissing(true);
    context.addLifecycleListener(appSpecificListener);

    SplibAppConfDirLifecycleListener sharedListener = new SplibAppConfDirLifecycleListener();
    // customPathPropertyName intentionally left unset: nothing overrides the shared directory.
    sharedListener.setDefaultPath(sharedDir.toString());
    sharedListener.setCreateDefaultPathIfMissing(true);
    context.addLifecycleListener(sharedListener);

    try {
      tomcat.start();

      // appSpecificDir is nested inside sharedDir, so creating it also creates sharedDir as a
      // side effect — both must nonetheless end up correctly mounted, in the right order.
      Files.writeString(appSpecificDir.resolve("marker.txt"), "from app-specific");
      Files.writeString(sharedDir.resolve("marker.txt"), "from shared");

      WebResource resource = context.getResources().getResource("/WEB-INF/classes/marker.txt");
      assertThat(resource.exists()).isTrue();
      assertThat(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
          .isEqualTo("from app-specific");
    } finally {
      tomcat.stop();
      tomcat.destroy();
    }
  }
}
