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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import jp.ecuacion.splib.core.http.SplibHttpClientSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@DisplayName("SplibHttpClientConfig")
class SplibHttpClientConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(SplibHttpClientConfig.class);

  @Test
  @DisplayName("the beans are registered")
  void beans() {
    runner.run(ctx -> {
      assertThat(ctx).hasSingleBean(SplibHttpClientSettings.class);
      assertThat(ctx).getBean(SplibHttpClientConfig.BEAN_NAME_HTTP_CLIENT)
          .isInstanceOf(HttpClient.class);
      assertThat(ctx).getBean(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY)
          .isInstanceOf(ClientHttpRequestFactory.class);
    });
  }

  @Test
  @DisplayName("the clients are lazy, so that apps not using them don't start a thread")
  void lazy() {
    runner.run(ctx -> {
      assertThat(ctx.getBeanFactory().getSingleton(SplibHttpClientConfig.BEAN_NAME_HTTP_CLIENT))
          .isNull();
      assertThat(ctx.getBeanFactory().getSingleton(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY))
          .isNull();
    });
  }

  @Test
  @DisplayName("the request factory isn't registered without spring-web, and startup succeeds")
  void withoutSpringWeb() {
    runner.withClassLoader(new FilteredClassLoader(ClientHttpRequestFactory.class)).run(ctx -> {
      assertThat(ctx).hasNotFailed();
      assertThat(ctx).hasBean(SplibHttpClientConfig.BEAN_NAME_HTTP_CLIENT);
      assertThat(ctx).doesNotHaveBean(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY);
    });
  }

  @Test
  @DisplayName("wrong settings fail at startup")
  void wrongSettings() {
    runner.withPropertyValues("jp.ecuacion.splib.core.http-client.proxy.host=proxy")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  @DisplayName("the read timeout is applied to the request factory")
  void readTimeout() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", exchange -> {
      try {
        release.await();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      exchange.close();
    });
    server.start();

    try {
      runner.withPropertyValues("jp.ecuacion.splib.core.http-client.read-timeout=100ms")
          .run(ctx -> {
            RestClient client = RestClient.builder()
                .requestFactory(ctx.getBean(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY,
                    ClientHttpRequestFactory.class))
                .build();
            String uri = "http://localhost:" + server.getAddress().getPort() + "/";

            assertThatThrownBy(() -> client.get().uri(uri).retrieve().toBodilessEntity())
                .isInstanceOf(ResourceAccessException.class);
          });
    } finally {
      release.countDown();
      server.stop(0);
    }
  }

  @Test
  @DisplayName("the JDK's client is used without proxy credentials")
  void jdkClientWithoutCredentials() {
    runner.run(ctx -> assertThat(ctx).getBean(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY)
        .isInstanceOf(JdkClientHttpRequestFactory.class));
  }

  @Nested
  @DisplayName("with proxy credentials")
  class WithProxyCredentials {

    private static final String KEY = "jp.ecuacion.splib.core.http-client.";

    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
    private final List<String> proxyAuthorizations =
        Collections.synchronizedList(new ArrayList<>());
    private final List<String> authorizations = Collections.synchronizedList(new ArrayList<>());
    private volatile int destStatus = 401;
    @SuppressWarnings("null")
    private HttpServer proxy;

    /** Starts a server acting as a proxy asking for credentials, then as the destination. */
    @BeforeEach
    void before() throws IOException {
      proxy = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      proxy.createContext("/", exchange -> {
        exchange.getRequestBody().readAllBytes();
        String proxyAuthorization = exchange.getRequestHeaders().getFirst("Proxy-Authorization");
        requests.add(exchange.getRequestMethod());
        proxyAuthorizations.add(String.valueOf(proxyAuthorization));
        authorizations.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));

        if (proxyAuthorization == null) {
          exchange.getResponseHeaders().add("Proxy-Authenticate", "Basic realm=\"proxy\"");
          exchange.sendResponseHeaders(407, -1);
        } else {
          byte[] body = "expired".getBytes(StandardCharsets.UTF_8);
          if (destStatus == 401) {
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"dest\"");
          }
          exchange.sendResponseHeaders(destStatus, body.length);
          exchange.getResponseBody().write(body);
        }
        exchange.close();
      });
      proxy.start();
    }

    @AfterEach
    void after() {
      proxy.stop(0);
    }

    private ApplicationContextRunner proxyRunner(int port) {
      return runner.withPropertyValues(KEY + "proxy.host=localhost", KEY + "proxy.port=" + port,
          KEY + "proxy.username=user", KEY + "proxy.password=pass");
    }

    private RestClient client(ApplicationContext ctx) {
      return RestClient.builder().requestFactory(ctx.getBean(
          SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY, ClientHttpRequestFactory.class)).build();
    }

    private String expectedProxyAuthorization() {
      return "Basic "
          + Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Apache HttpClient is used, and the JDK's client isn't created")
    void apacheHttpClient() {
      proxyRunner(proxy.getAddress().getPort()).run(ctx -> {
        assertThat(ctx).getBean(SplibHttpClientConfig.BEAN_NAME_REQUEST_FACTORY)
            .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
        assertThat(ctx.getBeanFactory().getSingleton(SplibHttpClientConfig.BEAN_NAME_HTTP_CLIENT))
            .isNull();
      });
    }

    @Test
    @DisplayName("startup fails without Apache HttpClient")
    void withoutApacheHttpClient() {
      proxyRunner(proxy.getAddress().getPort())
          .withClassLoader(new FilteredClassLoader("org.apache.hc.client5"))
          .run(ctx -> assertThat(ctx).hasFailed().getFailure().rootCause()
              .hasMessageContaining("httpclient5"));
    }

    @Test
    @DisplayName("credentials go only to the proxy, and a 401 to GET is returned with its body")
    void getUnauthorized() {
      proxyRunner(proxy.getAddress().getPort()).run(ctx -> {
        RestClient client = client(ctx);

        assertThatThrownBy(() -> client.get().uri("http://destination.invalid/").retrieve()
            .toBodilessEntity()).isInstanceOf(HttpClientErrorException.Unauthorized.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getResponseBodyAsString())
                .isEqualTo("expired"));
      });

      assertThat(proxyAuthorizations).contains(expectedProxyAuthorization());
      assertThat(authorizations).containsOnly("null");
    }

    @Test
    @DisplayName("a 401 to POST is returned with its body")
    void postUnauthorized() {
      proxyRunner(proxy.getAddress().getPort()).run(ctx -> {
        RestClient client = client(ctx);

        assertThatThrownBy(() -> client.post().uri("http://destination.invalid/").body("{}")
            .retrieve().toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.Unauthorized.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getResponseBodyAsString())
                .isEqualTo("expired"));
      });
    }

    @Test
    @DisplayName("PATCH is available")
    void patch() {
      destStatus = 200;

      proxyRunner(proxy.getAddress().getPort()).run(ctx -> {
        String body = client(ctx).patch().uri("http://destination.invalid/").body("{}").retrieve()
            .body(String.class);
        assertThat(body).isEqualTo("expired");
      });

      assertThat(requests).contains("PATCH");
    }

    @Test
    @DisplayName("HTTPS tunneling sends the credentials without the startup option of the JDK, "
        + "and the status of a refused CONNECT is returned as the response")
    void httpsTunneling() throws Exception {
      assertThat(System.getProperty("jdk.http.auth.tunneling.disabledSchemes")).isNull();
      List<String> connects = Collections.synchronizedList(new ArrayList<>());

      try (ServerSocket tunnelProxy = new ServerSocket(0, 5, InetAddress.getLoopbackAddress())) {
        Thread thread = new Thread(() -> answerTunneling(tunnelProxy, connects));
        thread.setDaemon(true);
        thread.start();

        proxyRunner(tunnelProxy.getLocalPort()).run(ctx -> {
          RestClient client = client(ctx);

          assertThatThrownBy(() -> client.get().uri("https://destination.invalid/").retrieve()
              .toBodilessEntity()).isInstanceOf(HttpClientErrorException.Forbidden.class);
        });
      }

      assertThat(connects).contains("auth=" + expectedProxyAuthorization());
    }

    private void answerTunneling(ServerSocket tunnelProxy, List<String> connects) {
      try {
        while (!tunnelProxy.isClosed()) {
          try (Socket socket = tunnelProxy.accept()) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String authorization = null;
            for (String line = reader.readLine(); line != null && !line.isEmpty();
                line = reader.readLine()) {
              if (line.toLowerCase(Locale.ROOT).startsWith("proxy-authorization:")) {
                authorization = line.substring(line.indexOf(':') + 1).trim();
              }
            }

            connects.add("auth=" + authorization);
            String response = authorization == null
                ? "HTTP/1.1 407 Auth\r\nProxy-Authenticate: Basic realm=\"p\"\r\n"
                    + "Connection: close\r\nContent-Length: 0\r\n\r\n"
                : "HTTP/1.1 403 Forbidden\r\nConnection: close\r\nContent-Length: 0\r\n\r\n";
            socket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
          }
        }
      } catch (IOException ex) {
        // The socket was closed at the end of the test.
      }
    }
  }
}
