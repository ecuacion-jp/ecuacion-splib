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
package jp.ecuacion.splib.core.util;

import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Clears the {@code PropertiesFileUtil} cache, and additionally refreshes spring's own
 * properties cache when {@code spring-cloud-context} is available.
 *
 * <p>{@code spring-cloud-context} is an optional dependency of {@code ecuacion-splib-core}
 *     (see its {@code pom.xml}). When a consuming app hasn't added it itself, {@link #clear()}
 *     still clears the {@code PropertiesFileUtil} cache but skips the spring-side refresh,
 *     logging that fact at INFO level instead of failing.</p>
 */
@Component
public class SplibPropertiesCacheClearer {

  private static final String CONTEXT_REFRESHER_CLASS_NAME =
      "org.springframework.cloud.context.refresh.ContextRefresher";

  private final DetailLogger detailLog = new DetailLogger(this);

  @Autowired
  private ApplicationContext applicationContext;

  /**
   * Clears the {@code PropertiesFileUtil} cache and, when {@code spring-cloud-context} is on
   * the classpath, also refreshes spring's own {@code @ConfigurationProperties} /
   * {@code @Value}-bound properties cache via {@code ContextRefresher}. Neither the
   * {@code /actuator/refresh} endpoint nor actuator itself is involved: {@code ContextRefresher}
   * is looked up and called directly.
   */
  public void clear() {
    PropertiesFileUtil.clearCache();

    if (ClassUtils.isPresent(CONTEXT_REFRESHER_CLASS_NAME, getClass().getClassLoader())) {
      SpringCloudContextRefreshInvoker.refresh(applicationContext);

    } else {
      detailLog.info("spring-cloud-context is not on the classpath, "
          + "so refreshing spring's properties cache was skipped.");
    }
  }

  /**
   * Isolates the direct reference to {@code ContextRefresher} in its own class so that
   * {@code SplibPropertiesCacheClearer} itself loads without error even when
   * spring-cloud-context is absent from the classpath; this class is only ever loaded from
   * {@link #clear()}, after that presence has already been confirmed.
   */
  private static final class SpringCloudContextRefreshInvoker {

    static void refresh(ApplicationContext applicationContext) {
      applicationContext.getBean(ContextRefresher.class).refresh();
    }
  }
}
