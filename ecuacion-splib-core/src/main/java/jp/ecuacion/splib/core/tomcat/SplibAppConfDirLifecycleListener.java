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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.jspecify.annotations.Nullable;

/**
 * A Tomcat {@link LifecycleListener}, referenced by a consuming app's own bundled {@code
 * META-INF/context.xml} via {@code <Listener className="...">}, that mounts a directory onto
 * that app's classpath (at {@link #webAppMount}, default {@code /WEB-INF/classes}) the same way
 * a static {@code <PreResources>} entry would — except the directory is created first if it
 * doesn't already exist, rather than failing context startup with an {@code
 * IllegalArgumentException} (Tomcat's own behavior for a {@code PreResources} whose {@code base}
 * is missing).
 *
 * <p><strong>Which directory gets mounted</strong> is resolved at {@link
 * Lifecycle#BEFORE_START_EVENT} (see the class-level timing note below) as follows:
 * <ol>
 *   <li>If {@link #customPathPropertyName} is set <em>and</em> that system property is itself
 *       set (and non-blank), the property's value is used, and the directory is
 *       <strong>always</strong> created if missing — an operator who explicitly points at a path
 *       clearly wants it to work.</li>
 *   <li>Otherwise, if {@link #defaultPath} is set, it's used, and the directory is created only
 *       if {@link #createDefaultPathIfMissing} is {@code true} (default {@code false} — an app
 *       opts in to this by setting the attribute in its own bundled {@code context.xml}; see the
 *       constructor-injected rationale below for why the default is conservative). If it's
 *       {@code false} and the directory doesn't exist, this resource is silently skipped
 *       (logged at INFO) rather than mounted with a path that would fail Tomcat's own validation
 *       moments later.</li>
 *   <li>Otherwise (no custom path in effect, and no {@link #defaultPath} configured at all),
 *       nothing is mounted (logged at INFO) — an app that wants "nothing happens unless an
 *       operator explicitly opts in" declares only {@link #customPathPropertyName} and leaves
 *       {@link #defaultPath} unset.</li>
 * </ol>
 *
 * <p>At least one of {@link #defaultPath} or {@link #customPathPropertyName} must be set —
 *     otherwise this {@code <Listener>} could never mount anything, which is always a
 *     misconfiguration. A single {@code context.xml} can declare more than one {@code <Listener>}
 *     of this class for the same {@link Context} — e.g. one for an app-specific directory with a
 *     configurable override, and a second, simpler one (with {@link #customPathPropertyName} left
 *     unset) for a fixed, shared directory. Listeners fire in declaration order, and {@link
 *     WebResourceRoot#addPreResources} appends — so a file present in more than one mounted
 *     directory resolves from whichever {@code <Listener>} was declared <em>first</em>, exactly
 *     as it would for two static {@code <PreResources>} entries in declaration order.</p>
 *
 * <p><strong>Example</strong> ({@code ecuacion-tool-command-api}'s bundled {@code context.xml}):
 * <pre>{@code
 * <Listener className="jp.ecuacion.splib.core.tomcat.SplibAppConfDirLifecycleListener"
 *     customPathPropertyName="jp.ecuacion.tool.command-api.app-conf-dir"
 *     defaultPath="${catalina.base}/app-conf/ecuacion-tool-command-api"
 *     createDefaultPathIfMissing="true"/>
 * }</pre>
 * {@code ${catalina.base}} above is expanded by Tomcat's own Digester property substitution
 * before this class ever sees the value (see {@code org.apache.tomcat.util.IntrospectionUtils}),
 * which also supports a {@code ${property:-default}} fallback syntax — so an app wanting a
 * configurable default path, not just a configurable override, can lean on that instead of (or
 * alongside) {@link #customPathPropertyName}.
 *
 * <p>An app that instead wants nothing mounted unless an operator explicitly opts in (e.g. {@code
 * ecuacion-tool-code-generator}'s bundled {@code context.xml}) leaves {@link #defaultPath}
 * unset entirely:
 * <pre>{@code
 * <Listener className="jp.ecuacion.splib.core.tomcat.SplibAppConfDirLifecycleListener"
 *     customPathPropertyName="jp.ecuacion.tool.code-generator.app-conf-dir"/>
 * }</pre>
 *
 * <p>Deploying with no Tomcat-side configuration at all is then a no-op for this listener — the
 * WAR's own embedded configuration is used as-is, with nothing created and nothing extra mounted.
 *
 * <p><strong>Lifecycle timing, verified against {@code tomcat-embed-core} 11.0.21 sources</strong>
 *     (not merely inferred — Tomcat's own reference docs don't cover this level of detail): a
 *     {@code <Listener>} declared in {@code META-INF/context.xml} is parsed and registered onto
 *     the {@link Context} — via {@code ContextRuleSet}'s {@code addLifecycleListener} digester
 *     rule — during {@code Context.init()} (triggered by {@code AFTER_INIT_EVENT}), which
 *     completes entirely before {@code Context.start()} is ever called. {@link
 *     Lifecycle#BEFORE_START_EVENT} fires from {@code LifecycleBase.start()} itself, before it
 *     calls {@code StandardContext.startInternal()} — and {@code startInternal()} is what calls
 *     {@code resourcesStart()}, which is what actually validates (and would otherwise reject) a
 *     missing {@code PreResources} directory. So a listener already registered during {@code
 *     init()} is guaranteed to see {@code BEFORE_START_EVENT} before that validation runs. At
 *     that point {@code context.getResources()} may already be non-null (if the app's {@code
 *     context.xml} also declares a static {@code <Resources>} block) or still null (if this
 *     listener is the only thing configuring resources); both cases are handled below.</p>
 */
public class SplibAppConfDirLifecycleListener implements LifecycleListener {

  private final DetailLogger detailLog = new DetailLogger(this);

  private @Nullable String customPathPropertyName;
  private @Nullable String defaultPath;
  private String webAppMount = "/WEB-INF/classes";
  private boolean createDefaultPathIfMissing = false;

  /**
   * The system property name an operator can set (e.g. via {@code -D} in a Tomcat {@code
   * setenv.sh} / {@code CATALINA_OPTS}) to override {@link #defaultPath} at deploy time. Optional
   * if {@link #defaultPath} is set — leave {@code customPathPropertyName} unset on {@code
   * <Listener>} in {@code context.xml} for a directory nothing should override. Left unset
   * together with {@link #defaultPath}, this {@code <Listener>} does nothing, which is always a
   * misconfiguration (see {@link #setDefaultPath}).
   */
  public void setCustomPathPropertyName(String customPathPropertyName) {
    this.customPathPropertyName = customPathPropertyName;
  }

  /**
   * The path used when {@link #customPathPropertyName} is unset, or set but not present as a
   * system property at deploy time. Optional if {@link #customPathPropertyName} is set — an app
   * that wants a directory mounted <em>only</em> when an operator explicitly opts in via that
   * property, and never otherwise, leaves {@code defaultPath} unset. At least one of the two must
   * be set. Set via the {@code defaultPath} attribute on {@code <Listener>} in {@code
   * context.xml} (typically starting with {@code ${catalina.base}}, expanded by Tomcat itself
   * before this class ever sees the value).
   */
  public void setDefaultPath(String defaultPath) {
    this.defaultPath = defaultPath;
  }

  /**
   * Where, within the web application, the resolved directory is mounted — passed straight
   * through to {@link DirResourceSet}'s {@code webAppMount}. Defaults to {@code
   * /WEB-INF/classes}, matching where {@code application.properties} et al. are conventionally
   * looked up on the classpath. Set via the {@code webAppMount} attribute on {@code <Listener>}.
   */
  public void setWebAppMount(String webAppMount) {
    this.webAppMount = webAppMount;
  }

  /**
   * Whether to create {@link #defaultPath} when it's in use (i.e. no custom path is in effect)
   * and doesn't already exist. Defaults to {@code false}: this listener is shared across every
   * app that might reference it, and a fixed, always-create default would silently start creating
   * directories on disk for apps that never opted into this behavior. An app that wants its own
   * default path auto-created (as opposed to only a custom, operator-supplied one) sets this
   * attribute to {@code true} itself — which requires {@link #defaultPath} to also be set, since
   * there would otherwise be no path to create. Set via the {@code createDefaultPathIfMissing}
   * attribute on {@code <Listener>}.
   */
  public void setCreateDefaultPathIfMissing(boolean createDefaultPathIfMissing) {
    this.createDefaultPathIfMissing = createDefaultPathIfMissing;
  }

  @Override
  public void lifecycleEvent(LifecycleEvent event) {
    if (!Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
      return;
    }

    if (defaultPath == null && customPathPropertyName == null) {
      throw new IllegalStateException("SplibAppConfDirLifecycleListener requires at least one of "
          + "'defaultPath' or 'customPathPropertyName' to be set as a <Listener> attribute in "
          + "context.xml — otherwise it could never mount anything.");
    }

    if (createDefaultPathIfMissing && defaultPath == null) {
      throw new IllegalStateException("SplibAppConfDirLifecycleListener has "
          + "'createDefaultPathIfMissing=true' but no 'defaultPath' is set as a <Listener> "
          + "attribute in context.xml — there is no default path to create.");
    }

    String customPath = customPathPropertyName == null ? null
        : System.getProperty(Objects.requireNonNull(customPathPropertyName));
    boolean usingCustomPath = customPath != null && !customPath.isBlank();

    String resolvedPath;
    boolean shouldCreate;
    if (usingCustomPath) {
      resolvedPath = customPath;
      shouldCreate = true;

    } else if (defaultPath != null) {
      resolvedPath = defaultPath;
      shouldCreate = createDefaultPathIfMissing;

    } else {
      detailLog.info("'" + customPathPropertyName + "' is not set and no 'defaultPath' is "
          + "configured, so nothing will be mounted onto '" + webAppMount + "'.");
      return;
    }

    File dir = new File(Objects.requireNonNull(resolvedPath));
    if (!dir.isDirectory()) {
      if (!shouldCreate) {
        detailLog.info("'" + resolvedPath + "' does not exist and createDefaultPathIfMissing is "
            + "false, so it will not be mounted onto '" + webAppMount + "'.");
        return;
      }

      try {
        Files.createDirectories(dir.toPath());
        detailLog.info("Created '" + resolvedPath + "' to mount onto '" + webAppMount + "'.");
      } catch (IOException e) {
        throw new IllegalStateException(
            "Failed to create '" + resolvedPath + "' to mount " + "onto '" + webAppMount + "'.", e);
      }
    }

    Context context = (Context) event.getLifecycle();
    WebResourceRoot resources = context.getResources();
    if (resources == null) {
      resources = new StandardRoot(context);
      context.setResources(resources);
    }

    DirResourceSet resourceSet = new DirResourceSet(resources, webAppMount, resolvedPath, "/");
    resourceSet.setReadOnly(true);
    resources.addPreResources(resourceSet);
  }
}
