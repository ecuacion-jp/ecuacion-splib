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

import java.util.Objects;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * Registers ecuacion module {@code application_*.properties} files
 * (e.g., {@code application_splib_web.properties}) into Spring's {@link ConfigurableEnvironment}.
 *
 * <p>Added at the end of the {@link org.springframework.core.env.PropertySources} chain,
 * so values in the application's own {@code application.properties} take precedence.</p>
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor}.</p>
 */
public class SplibEnvironmentPostProcessor implements EnvironmentPostProcessor {

  /**
   * Constructs a new instance.
   */
  public SplibEnvironmentPostProcessor() {}

  /**
   * Adds a {@link PropertySource} backed by {@link PropertiesFileUtil} to the environment.
   *
   * @param environment the environment to post-process
   * @param application the application to post-process
   */
  @Override
  public void postProcessEnvironment(@Nullable ConfigurableEnvironment environment,
      @Nullable SpringApplication application) {
    Objects.requireNonNull(environment).getPropertySources()
        .addLast(new ApplicationPropertySource());
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
     * @param name the property name
     * @return the property value, or {@code null} if not found
     */
    @Override
    public @Nullable Object getProperty(@Nullable String name) {
      Objects.requireNonNull(name);
      return PropertiesFileUtil.hasApplication(name) ? PropertiesFileUtil.getApplication(name)
          : null;
    }
  }
}
