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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;

@DisplayName("SplibHttpClientSettings")
class SplibHttpClientSettingsTest {

  private static final String PREFIX = "jp.ecuacion.splib.core.http-client.";
  private static final String EXPECTED_PROXY_AUTHORIZATION = "Basic "
      + Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));

  @Nested
  @DisplayName("settings")
  class Settings {

    @Test
    @DisplayName("timeouts have defaults when not set")
    void defaultTimeouts() {
      SplibHttpClientSettings settings = new SplibHttpClientSettings(new MockEnvironment());

      assertThat(settings.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
      assertThat(settings.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("timeouts accept the duration format of Spring Boot")
    void timeouts() {
      SplibHttpClientSettings settings = new SplibHttpClientSettings(
          new MockEnvironment().withProperty(PREFIX + "connect-timeout", "3s")
              .withProperty(PREFIX + "read-timeout", "1m"));

      assertThat(settings.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
      assertThat(settings.getReadTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

    @ParameterizedTest
    @CsvSource({"proxy.host,proxy", "proxy.port,8080", "connect-timeout,0s", "read-timeout,-1s"})
    @DisplayName("inconsistent settings fail")
    void inconsistent(String key, String value) {
      MockEnvironment env = new MockEnvironment().withProperty(PREFIX + key, value);

      assertThatThrownBy(() -> new SplibHttpClientSettings(env))
          .isInstanceOf(IllegalStateException.class).hasMessageContaining(PREFIX);
    }

    @Test
    @DisplayName("username without the proxy fails")
    void usernameWithoutProxy() {
      MockEnvironment env = new MockEnvironment().withProperty(PREFIX + "proxy.username", "user")
          .withProperty(PREFIX + "proxy.password", "pass");

      assertThatThrownBy(() -> new SplibHttpClientSettings(env))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("username without password fails")
    void usernameWithoutPassword() {
      MockEnvironment env = new MockEnvironment().withProperty(PREFIX + "proxy.host", "proxy")
          .withProperty(PREFIX + "proxy.port", "8080")
          .withProperty(PREFIX + "proxy.username", "user");

      assertThatThrownBy(() -> new SplibHttpClientSettings(env))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("empty values are treated as not set")
    void emptyValues() {
      SplibHttpClientSettings settings = new SplibHttpClientSettings(
          new MockEnvironment().withProperty(PREFIX + "proxy.host", "")
              .withProperty(PREFIX + "proxy.port", "")
              .withProperty(PREFIX + "proxy.username", "")
              .withProperty(PREFIX + "proxy.password", ""));

      assertThat(settings.buildHttpClient().proxy()).isEmpty();
    }
  }

  @Nested
  @DisplayName("buildHttpClient()")
  class BuildHttpClient {

    private final List<String> requestUris = Collections.synchronizedList(new ArrayList<>());
    private final List<String> proxyAuthorizations =
        Collections.synchronizedList(new ArrayList<>());
    private final List<String> authorizations = Collections.synchronizedList(new ArrayList<>());

    @SuppressWarnings("null")
    private HttpServer proxy;
    private volatile int destStatus = 200;

    /** Starts a server acting as a proxy asking for credentials, then as the destination. */
    @BeforeEach
    void before() throws IOException {
      proxy = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      proxy.createContext("/", exchange -> {
        String proxyAuthorization = exchange.getRequestHeaders().getFirst("Proxy-Authorization");
        requestUris.add(exchange.getRequestURI().toString());
        proxyAuthorizations.add(String.valueOf(proxyAuthorization));
        authorizations.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));

        if (proxyAuthorization == null) {
          exchange.getResponseHeaders().add("Proxy-Authenticate", "Basic realm=\"proxy\"");
          exchange.sendResponseHeaders(407, -1);
        } else {
          if (destStatus == 401) {
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"dest\"");
          }
          exchange.sendResponseHeaders(destStatus, -1);
        }
        exchange.close();
      });
      proxy.start();
    }

    @AfterEach
    void after() {
      proxy.stop(0);
    }

    private MockEnvironment proxyEnv() {
      return new MockEnvironment().withProperty(PREFIX + "proxy.host", "localhost")
          .withProperty(PREFIX + "proxy.port", String.valueOf(proxy.getAddress().getPort()));
    }

    private HttpResponse<Void> send(MockEnvironment env) throws Exception {
      try (HttpClient client = new SplibHttpClientSettings(env).buildHttpClient()) {
        return client.send(
            HttpRequest.newBuilder(URI.create("http://destination.invalid/path")).build(),
            HttpResponse.BodyHandlers.discarding());
      }
    }

    @Test
    @DisplayName("the JVM's default proxy selector is used when the proxy isn't set")
    void noProxy() {
      HttpClient client = new SplibHttpClientSettings(new MockEnvironment()).buildHttpClient();

      assertThat(client.proxy()).isEmpty();
      assertThat(client.authenticator()).isEmpty();
    }

    @Test
    @DisplayName("requests go through the proxy")
    void throughProxy() throws Exception {
      HttpResponse<Void> response = send(proxyEnv());

      // No credentials are set, so the 407 is returned as it is.
      assertThat(response.statusCode()).isEqualTo(407);
      assertThat(requestUris).containsExactly("http://destination.invalid/path");
    }

    @Test
    @DisplayName("credentials are sent to the proxy when it asks for them")
    void credentialsToProxy() throws Exception {
      HttpResponse<Void> response = send(proxyEnv().withProperty(PREFIX + "proxy.username", "user")
          .withProperty(PREFIX + "proxy.password", "pass"));

      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(proxyAuthorizations).containsExactly("null", EXPECTED_PROXY_AUTHORIZATION);
    }

    @Test
    @DisplayName("credentials aren't sent to the destination, and its 401 throws IOException")
    void noCredentialsToDestination() {
      destStatus = 401;
      MockEnvironment env = proxyEnv().withProperty(PREFIX + "proxy.username", "user")
          .withProperty(PREFIX + "proxy.password", "pass");

      // A limitation of the JDK's HttpClient. See the Javadoc of buildHttpClient().
      assertThatThrownBy(() -> send(env)).isInstanceOf(IOException.class);
      assertThat(authorizations).containsOnly("null");
    }
  }
}
