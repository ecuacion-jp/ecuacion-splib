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
package jp.ecuacion.splib.core.http;

import java.net.URI;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Creates the request factory used when proxy credentials are set, with Apache HttpClient.
 *
 * <p>The JDK's clients aren't used in that case. The credentials are scoped to the proxy here,
 *     so a 401 from the destination is returned with its body, every HTTP method is available
 *     and {@code jdk.http.auth.tunneling.disabledSchemes} isn't needed.</p>
 *
 * <p>Apache HttpClient is an optional dependency,
 *     so this class must be used only when it's on the classpath.</p>
 */
public final class SplibApacheHttpRequestFactories {

  private SplibApacheHttpRequestFactories() {}

  /**
   * Creates the request factory.
   *
   * @param settings settings with the proxy credentials
   * @return ClientHttpRequestFactory
   */
  public static ClientHttpRequestFactory create(SplibHttpClientSettings settings) {
    String host = settings.requireProxyHost();
    int port = settings.requireProxyPort();

    BasicCredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(
        settings.requireProxyUsername(), settings.requireProxyPassword().toCharArray()));

    ConnectionConfig connectionConfig =
        ConnectionConfig.custom().setConnectTimeout(Timeout.of(settings.getConnectTimeout()))
            .setSocketTimeout(Timeout.of(settings.getReadTimeout())).build();

    return new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
        .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig).build())
        .setProxy(new HttpHost(host, port)).setDefaultCredentialsProvider(credentials)
        .setRedirectStrategy(redirectStrategy()).disableCookieManagement().build());
  }

  /**
   * Creates the redirect strategy, which doesn't follow redirects from HTTPS to HTTP,
   * same as the JDK's client used without proxy credentials.
   *
   * @return RedirectStrategy
   */
  static RedirectStrategy redirectStrategy() {
    return new DefaultRedirectStrategy() {
      @Override
      public boolean isRedirected(@SuppressWarnings("null") HttpRequest request,
          @SuppressWarnings("null") HttpResponse response,
          @SuppressWarnings("null") HttpContext context) throws ProtocolException {
        if (!super.isRedirected(request, response, context)) {
          return false;
        }

        try {
          URI location = getLocationURI(request, response, context);
          return !("https".equalsIgnoreCase(request.getScheme())
              && "http".equalsIgnoreCase(location.getScheme()));

        } catch (HttpException ex) {
          throw new ProtocolException(ex.getMessage(), ex);
        }
      }
    };
  }
}
