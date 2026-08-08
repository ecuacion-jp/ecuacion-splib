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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Performs three unrelated pieces of early {@link ConfigurableEnvironment} setup that all
 * need to run as a Spring Boot {@link EnvironmentPostProcessor} (i.e. before most of the
 * application context is available):
 *
 * <ol>
 *   <li>Registers ecuacion module {@code application_*.properties} files
 *       (e.g., {@code application_splib_web.properties}) into the {@link
 *       ConfigurableEnvironment}. See {@link #postProcessEnvironment} and {@link
 *       ApplicationPropertySource}.</li>
 *   <li>Registers the reverse bridge: lets {@link PropertiesFileUtil}'s own
 *       {@code application.properties} reading (which only knows the classpath) fall back to
 *       this application's {@link ConfigurableEnvironment} — which, by the time this class
 *       runs, already has Spring Boot's own externalized configuration merged in (e.g.
 *       {@code file:./config/application.properties} next to an executable jar/war). See
 *       {@link #postProcessEnvironment}.</li>
 *   <li>Falls back to {@code config/logback-spring.xml}, or else {@code logback-spring.xml}
 *       directly in the current working directory, for logging configuration when nothing
 *       has set {@code logging.config} already. See {@link #addLogbackConfigFallback}.</li>
 * </ol>
 *
 * <p>Registered via the {@code org.springframework.boot.EnvironmentPostProcessor} key in
 * {@code META-INF/spring.factories} — {@code EnvironmentPostProcessor} is one of the few
 * remaining Spring Boot extension points not discovered via a {@code *.imports} file, so it
 * must go through the classic {@code spring.factories} mechanism instead.</p>
 */
public class SplibEnvironmentPostProcessor implements EnvironmentPostProcessor {

  /** The Spring Boot property that points at a custom logback configuration file. */
  private static final String LOGGING_CONFIG_PROPERTY_KEY = "logging.config";

  /**
   * Constructs a new instance.
   */
  public SplibEnvironmentPostProcessor() {}

  /**
   * Adds a {@link PropertySource} backed by {@link PropertiesFileUtil} to the environment,
   * registers the reverse bridge so {@link PropertiesFileUtil}'s own {@code
   * application.properties} reading can fall back to this {@code environment} (see {@link
   * #registerApplicationEnvironmentFallback}), and (see {@link #addLogbackConfigFallback})
   * makes {@code config/logback-spring.xml} (or a root-level {@code logback-spring.xml}) work
   * without needing {@code -Dlogging.config} on the command line.
   *
   * @param environment the environment to post-process
   * @param application the application to post-process
   */
  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment,
      SpringApplication application) {
    environment.getPropertySources().addLast(new ApplicationPropertySource());
    registerApplicationEnvironmentFallback(environment);
    addLogbackConfigFallback(environment);
  }

  /**
   * Lets {@link PropertiesFileUtil#getApplication(String)} (and friends, e.g. {@link
   * jp.ecuacion.lib.core.util.LocaleUtil}) see values from outside the classpath — most
   * notably Spring Boot's own externalized {@code application.properties} locations (e.g.
   * {@code file:./config/application.properties} next to an executable jar/war), which by the
   * time this method runs have already been merged into {@code environment} by {@code
   * ConfigDataEnvironmentPostProcessor} (a higher-priority {@link EnvironmentPostProcessor}
   * than this class).
   *
   * <p>ecuacion-lib itself never depends on Spring; it only exposes an extension point ({@link
   * PropertiesFileUtil#setApplicationEnvironmentFallbackResolver}) for a framework-specific
   * module such as this one to plug into. A value returned here is consulted after an explicit
   * JVM system property ({@code -D}) but before ecuacion-lib's own classpath-bundled {@code
   * application.properties}, so an externally placed {@code application.properties} can
   * override the classpath default the same way it already does for Spring-native properties
   * (e.g. {@code spring.*}).</p>
   *
   * <p>Registered as a plain lambda delegating to {@code environment::getProperty} rather than
   * a reference to {@code environment} itself: the {@link PropertiesFileUtil} side stores this
   * resolver in a single static field, so whichever {@link ConfigurableEnvironment} registered
   * here last is the one every caller in the JVM sees. That is a known limitation shared with
   * {@link PropertiesFileUtil#setExternalPlaceholderResolver}, and matters only when more than
   * one Spring context is bootstrapped concurrently in the same JVM (e.g. parallel tests).</p>
   *
   * @param environment the environment to fall back to
   */
  private void registerApplicationEnvironmentFallback(ConfigurableEnvironment environment) {
    PropertiesFileUtil.setApplicationEnvironmentFallbackResolver(environment::getProperty);
  }

  /**
   * Makes {@code config/logback-spring.xml}, or (if that is absent) {@code logback-spring.xml}
   * directly in the current working directory — in practice, the directory the executable
   * jar/war is launched from — work as a drop-in logging configuration, without requiring
   * {@code -Dlogging.config=...} on the command line.
   *
   * <p><b>Why this method exists:</b> Spring Boot auto-discovers {@code
   * config/application.properties} next to the running jar/war on its own (that discovery
   * is done by {@code ConfigDataEnvironmentPostProcessor}, a higher-priority {@link
   * EnvironmentPostProcessor} than this class, so it has already run by the time this method
   * executes). Boot does <b>not</b> do the equivalent for {@code logback-spring.xml}: unless
   * {@code logging.config} explicitly points at a file, Boot only looks on the classpath, and
   * a {@code config/} directory sitting next to the jar/war is not on the classpath. Without
   * this method, users who want to override logging would have to pass {@code
   * -Dlogging.config=file:./config/logback-spring.xml} by hand even though the equivalent
   * {@code application.properties} override needs no such flag — an inconsistency this method
   * papers over.</p>
   *
   * <p><b>How it works / why the timing is safe:</b> This class does not implement {@link
   * org.springframework.core.Ordered}, so it runs at default (lowest) precedence among {@code
   * EnvironmentPostProcessor}s, strictly after {@code ConfigDataEnvironmentPostProcessor}.
   * That means by the time this method runs, any {@code logging.config} set via a JVM system
   * property, a command-line argument, or {@code config/application.properties} /
   * {@code application.properties} has already been merged into {@code environment}, so
   * {@link ConfigurableEnvironment#containsProperty} below reliably detects it and this
   * fallback backs off. On the other hand, {@code EnvironmentPostProcessor}s as a whole run
   * on the same {@code ApplicationEnvironmentPreparedEvent} but at a higher priority than
   * {@code LoggingApplicationListener} (the listener that actually reads {@code
   * logging.config} and initializes Logback), so a property added here still arrives in time
   * to influence that initialization. In short: this method has exactly one chance to run,
   * and it is guaranteed to be after every other {@code logging.config} source and before
   * Logback itself reads it.</p>
   *
   * <p>If no explicit {@code logging.config} was found and neither {@code
   * config/logback-spring.xml} nor a root-level {@code logback-spring.xml} exists, this method
   * does nothing and Boot's normal (classpath-only) logging configuration lookup proceeds
   * unchanged.</p>
   *
   * @param environment the environment to post-process
   */
  private void addLogbackConfigFallback(ConfigurableEnvironment environment) {
    // Someone already resolved logging.config (system property, CLI arg, or an
    // application.properties, including config/application.properties) — leave it alone.
    if (environment.containsProperty(LOGGING_CONFIG_PROPERTY_KEY)) {
      return;
    }

    File logbackConfigFile = findLogbackConfigFile();
    if (logbackConfigFile == null) {
      return;
    }

    Map<String, Object> fallbackProperty = new HashMap<>();
    fallbackProperty.put(LOGGING_CONFIG_PROPERTY_KEY,
        "file:" + logbackConfigFile.getAbsolutePath());
    environment.getPropertySources()
        .addLast(new MapPropertySource("splibLogbackConfigFallback", fallbackProperty));
  }

  /**
   * Looks for a {@code logback-spring.xml} to fall back to, preferring {@code config/}
   * (matching where {@code application.properties} is conventionally overridden) over the
   * working directory root.
   *
   * @return the file found, or {@code null} if neither location has one
   */
  private @Nullable File findLogbackConfigFile() {
    File userDir = new File(Objects.requireNonNull(System.getProperty("user.dir")));

    File configDirFile = new File(userDir, "config/logback-spring.xml");
    if (configDirFile.isFile()) {
      return configDirFile;
    }

    File rootFile = new File(userDir, "logback-spring.xml");
    return rootFile.isFile() ? rootFile : null;
  }

  /**
   * Delegates property lookups to {@link PropertiesFileUtil#getApplication(String)}.
   *
   * <p>Returns {@code null} when the key is not found, allowing higher-priority
   * sources in the chain to handle it.</p>
   */
  private static class ApplicationPropertySource extends PropertySource<Object> {

    /** The name used to identify this {@link PropertySource} in the environment. */
    static final String SOURCE_NAME = "splibPropertiesFileUtil";

    /**
     * Constructs a new instance.
     */
    ApplicationPropertySource() {
      super(SOURCE_NAME, new Object());
    }

    /**
     * Returns the value for the given property name from {@link PropertiesFileUtil}.
     *
     * <p>Uses {@link PropertiesFileUtil
     * #getApplicationIfExistsWithoutExternalPlaceholderResolution} rather than {@link
     * PropertiesFileUtil#getApplication(String)}, and rather than checking {@link
     * PropertiesFileUtil#hasApplication(String)} separately beforehand: this {@code
     * PropertySource} is itself consulted from within (a) Spring's own {@code ${...}}
     * placeholder resolution, which ecuacion-lib's external placeholder resolver hook delegates
     * to, and (b) this same class's application-environment fallback resolver (see {@link
     * #registerApplicationEnvironmentFallback}), which queries this very {@code Environment}.
     * Re-applying either hook here — including via a separate, unsuppressed {@code
     * hasApplication} pre-check — would recurse back into this same {@code Environment}
     * indefinitely instead of letting the single resolution pass that reached this method
     * continue.</p>
     *
     * @param name the property name
     * @return the property value, or {@code null} if not found
     */
    @Override
    public @Nullable Object getProperty(String name) {
      return PropertiesFileUtil.getApplicationIfExistsWithoutExternalPlaceholderResolution(name);
    }
  }
}
