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

import java.net.http.HttpClient;
import jp.ecuacion.splib.core.http.SplibApacheHttpRequestFactories;
import jp.ecuacion.splib.core.http.SplibHttpClientSettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.ClassUtils;

/**
 * Registers the HTTP client that applications use to call external services.
 *
 * <p>Use the bean {@value #BEAN_NAME_REQUEST_FACTORY} for {@code RestClient} and
 *     {@code RestTemplate}, and the bean {@value #BEAN_NAME_HTTP_CLIENT} for the JDK's
 *     {@code HttpClient}. See {@link SplibHttpClientSettings} for the settings.</p>
 *
 * <p>The clients are lazy since creating an {@code HttpClient} starts a thread,
 *     which apps not calling external services don't need.</p>
 */
@Configuration(value = "splibHttpClientConfig", proxyBeanMethods = false)
public class SplibHttpClientConfig {

  /** The bean name of {@link HttpClient}. */
  public static final String BEAN_NAME_HTTP_CLIENT = "splibHttpClient";

  /** The bean name of {@code ClientHttpRequestFactory}. */
  public static final String BEAN_NAME_REQUEST_FACTORY = "splibClientHttpRequestFactory";

  /**
   * Provides {@link SplibHttpClientSettings}. It's not lazy so that wrong settings fail at startup.
   *
   * @param env the Spring environment
   * @return SplibHttpClientSettings
   */
  @Bean
  SplibHttpClientSettings splibHttpClientSettings(Environment env) {
    return new SplibHttpClientSettings(env);
  }

  /**
   * Provides {@link HttpClient}.
   *
   * @param settings settings
   * @return HttpClient
   */
  @Bean(name = BEAN_NAME_HTTP_CLIENT, destroyMethod = "shutdownNow")
  @Lazy
  HttpClient splibHttpClient(SplibHttpClientSettings settings) {
    return settings.buildHttpClient();
  }

  /** Registers the beans depending on {@code spring-web}, which is an optional dependency. */
  @Configuration(value = "splibHttpClientRequestFactoryConfig", proxyBeanMethods = false)
  @ConditionalOnClass(name = "org.springframework.http.client.ClientHttpRequestFactory")
  static class RequestFactoryConfig {

    private static final String APACHE_HTTP_CLIENT_CLASS =
        "org.apache.hc.client5.http.impl.classic.HttpClients";

    /**
     * Constructs a new instance with {@code settings}.
     *
     * @param settings settings
     * @param resourceLoader resourceLoader to get the class loader of the app
     * @throws IllegalStateException if proxy credentials are set without Apache HttpClient,
     *     so that it fails at startup although the bean is lazy
     */
    RequestFactoryConfig(SplibHttpClientSettings settings, ResourceLoader resourceLoader) {
      if (settings.hasProxyCredentials()
          && !ClassUtils.isPresent(APACHE_HTTP_CLIENT_CLASS, resourceLoader.getClassLoader())) {
        throw new IllegalStateException("Proxy credentials "
            + "(jp.ecuacion.splib.core.http-client.proxy.username) require "
            + "org.apache.httpcomponents.client5:httpclient5. Add it to the app's dependencies.");
      }
    }

    /**
     * Provides {@link ClientHttpRequestFactory}.
     *
     * <p>It uses the {@link HttpClient} bean, unless proxy credentials are set. With them
     *     {@link HttpClient} throws {@code IOException} for a 401 from the destination, which
     *     hides the cause (e.g. an expired API key) behind a message about credentials.
     *     So Apache HttpClient is used instead. Note that when the proxy refuses CONNECT,
     *     it returns the status of the proxy as the response instead of throwing an exception.</p>
     *
     * @param httpClient httpClient
     * @param settings settings
     * @return ClientHttpRequestFactory
     */
    @Bean(BEAN_NAME_REQUEST_FACTORY)
    @Lazy
    ClientHttpRequestFactory splibClientHttpRequestFactory(
        @Qualifier(BEAN_NAME_HTTP_CLIENT) ObjectProvider<HttpClient> httpClient,
        SplibHttpClientSettings settings) {
      if (settings.hasProxyCredentials()) {
        return SplibApacheHttpRequestFactories.create(settings);
      }

      JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient.getObject());
      factory.setReadTimeout(settings.getReadTimeout());
      return factory;
    }
  }
}
