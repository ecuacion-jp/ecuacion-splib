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

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Provides the settings of the outbound HTTP client, and builds the client from them.
 *
 * <p>The settings are read from the following properties and are applied only to the clients
 *     built here, not to the whole JVM.</p>
 * <ul>
 *   <li>{@code jp.ecuacion.splib.core.http-client.proxy.host} / {@code .port}:
 *       the proxy. When omitted, the JVM's default ({@code https.proxyHost} etc.) is used.</li>
 *   <li>{@code jp.ecuacion.splib.core.http-client.proxy.username} / {@code .password}:
 *       the credentials of the proxy, optional.</li>
 *   <li>{@code jp.ecuacion.splib.core.http-client.connect-timeout}: 10 seconds by default.</li>
 *   <li>{@code jp.ecuacion.splib.core.http-client.read-timeout}: 30 seconds by default.</li>
 * </ul>
 *
 * <p>When proxy credentials are set, {@code ClientHttpRequestFactory} needs Apache HttpClient
 *     (see {@link SplibApacheHttpRequestFactories}). The JDK's {@link HttpClient} doesn't,
 *     but it needs {@code -Djdk.http.auth.tunneling.disabledSchemes=""} in the startup options,
 *     since the JDK disables Basic authentication for HTTPS tunneling through a proxy (CONNECT)
 *     by default. It's a JVM-wide setting, so it's left to the app,
 *     and a warning is logged when the client is built without it.</p>
 */
public class SplibHttpClientSettings {

  private static final String PREFIX = "jp.ecuacion.splib.core.http-client.";
  private static final String PROP_KEY_PROXY_HOST = PREFIX + "proxy.host";
  private static final String PROP_KEY_PROXY_PORT = PREFIX + "proxy.port";
  private static final String PROP_KEY_PROXY_USERNAME = PREFIX + "proxy.username";
  private static final String PROP_KEY_PROXY_PASSWORD = PREFIX + "proxy.password";
  private static final String PROP_KEY_CONNECT_TIMEOUT = PREFIX + "connect-timeout";
  private static final String PROP_KEY_READ_TIMEOUT = PREFIX + "read-timeout";
  private static final String SYS_PROP_TUNNELING_DISABLED_SCHEMES =
      "jdk.http.auth.tunneling.disabledSchemes";

  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

  private final DetailLogger detailLog = new DetailLogger(this);

  private final @Nullable String proxyHost;
  private final @Nullable Integer proxyPort;
  private final @Nullable String proxyUsername;
  private final @Nullable String proxyPassword;
  private final Duration connectTimeout;
  private final Duration readTimeout;

  /**
   * Constructs a new instance with {@code env}.
   *
   * @param env the Spring environment
   * @throws IllegalStateException if the settings are inconsistent
   */
  public SplibHttpClientSettings(Environment env) {
    proxyHost = textOrNull(env.getProperty(PROP_KEY_PROXY_HOST));
    proxyPort = env.getProperty(PROP_KEY_PROXY_PORT, Integer.class);
    proxyUsername = textOrNull(env.getProperty(PROP_KEY_PROXY_USERNAME));
    proxyPassword = textOrNull(env.getProperty(PROP_KEY_PROXY_PASSWORD));
    connectTimeout = timeout(env, PROP_KEY_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
    readTimeout = timeout(env, PROP_KEY_READ_TIMEOUT, DEFAULT_READ_TIMEOUT);

    if ((proxyHost == null) != (proxyPort == null)) {
      throw new IllegalStateException(
          "'" + PROP_KEY_PROXY_HOST + "' and '" + PROP_KEY_PROXY_PORT + "' must be set together.");
    }

    if (proxyUsername != null && proxyHost == null) {
      throw new IllegalStateException(
          "'" + PROP_KEY_PROXY_USERNAME + "' requires '" + PROP_KEY_PROXY_HOST + "'.");
    }

    if ((proxyUsername == null) != (proxyPassword == null)) {
      throw new IllegalStateException("'" + PROP_KEY_PROXY_USERNAME + "' and '"
          + PROP_KEY_PROXY_PASSWORD + "' must be set together.");
    }
  }

  /**
   * Builds a new {@link HttpClient} with the proxy and the connect timeout.
   *
   * <p>The read timeout isn't a setting of {@link HttpClient}. To apply it, pass
   *     {@link #getReadTimeout()} to {@code HttpRequest.Builder#timeout}.</p>
   *
   * <p>Redirects are followed except from HTTPS to HTTP, same as {@code RestTemplate}'s default
   *     for GET. Proxy credentials are never sent to the destination.</p>
   *
   * <p>When proxy credentials are set, a 401 from the destination throws {@code IOException}
   *     ({@code No credentials provided} etc.) instead of returning the response, since
   *     {@link HttpClient} can't apply an authenticator only to proxies. Prefer the
   *     {@code ClientHttpRequestFactory} bean of {@code SplibHttpClientConfig},
   *     which returns the 401, when the destination may return it.</p>
   *
   * @return the HttpClient
   */
  public HttpClient buildHttpClient() {
    if (hasProxyCredentials() && isBasicTunnelingDisabled()) {
      detailLog.warn("Proxy credentials are set, but Basic authentication for HTTPS tunneling "
          + "is disabled by the JDK. Add -D" + SYS_PROP_TUNNELING_DISABLED_SCHEMES
          + "=\"\" to the startup options, or HTTPS requests of HttpClient fail with 407.");
    }

    HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(connectTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL);

    String host = proxyHost;
    Integer port = proxyPort;
    if (host != null && port != null) {
      builder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
    }

    String username = proxyUsername;
    String password = proxyPassword;
    if (username != null && password != null) {
      builder.authenticator(new ProxyAuthenticator(username, password));
    }

    return builder.build();
  }

  /**
   * Returns whether proxy credentials are set.
   *
   * @return {@code true} if proxy credentials are set
   */
  public boolean hasProxyCredentials() {
    return proxyUsername != null;
  }

  String requireProxyHost() {
    return Objects.requireNonNull(proxyHost);
  }

  int requireProxyPort() {
    return Objects.requireNonNull(proxyPort);
  }

  String requireProxyUsername() {
    return Objects.requireNonNull(proxyUsername);
  }

  String requireProxyPassword() {
    return Objects.requireNonNull(proxyPassword);
  }

  /**
   * Gets the connect timeout.
   *
   * @return the connect timeout
   */
  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Gets the read timeout.
   *
   * @return the read timeout
   */
  public Duration getReadTimeout() {
    return readTimeout;
  }

  private static @Nullable String textOrNull(@Nullable String value) {
    return StringUtils.hasText(value) ? value : null;
  }

  @SuppressWarnings("null")
  private static Duration timeout(Environment env, String key, Duration defaultValue) {
    @NonNull
    Duration value = Binder.get(env).bind(key, Duration.class).orElse(defaultValue);

    if (value.isZero() || value.isNegative()) {
      throw new IllegalStateException("'" + key + "' must be positive.");
    }

    return value;
  }

  private static boolean isBasicTunnelingDisabled() {
    // The JDK's default of this property, in net.properties, includes Basic.
    String schemes = System.getProperty(SYS_PROP_TUNNELING_DISABLED_SCHEMES);
    return schemes == null || schemes.toLowerCase(Locale.ROOT).contains("basic");
  }

  /** Answers only to proxies, so that the credentials never go to the destination. */
  private static final class ProxyAuthenticator extends Authenticator {

    private final String username;
    private final String password;

    private ProxyAuthenticator(String username, String password) {
      this.username = username;
      this.password = password;
    }

    @Override
    protected @Nullable PasswordAuthentication getPasswordAuthentication() {
      if (getRequestorType() != RequestorType.PROXY) {
        return null;
      }

      return new PasswordAuthentication(username, password.toCharArray());
    }
  }
}
