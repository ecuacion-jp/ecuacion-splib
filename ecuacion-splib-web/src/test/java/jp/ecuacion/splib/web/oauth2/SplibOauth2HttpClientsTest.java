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
package jp.ecuacion.splib.web.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.web.client.RestOperations;

@DisplayName("SplibOauth2HttpClients")
class SplibOauth2HttpClientsTest {

  private static final String ERROR_BODY = "{\"error\":\"invalid_grant\"}";

  @SuppressWarnings("null")
  private HttpServer server;
  @SuppressWarnings("null")
  private String uri;
  private volatile int status = 200;
  private volatile String body = "";

  @BeforeEach
  void before() throws Exception {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", exchange -> {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    uri = "http://localhost:" + server.getAddress().getPort() + "/";
  }

  @AfterEach
  void after() {
    server.stop(0);
  }

  private void respond(int status, String body) {
    this.status = status;
    this.body = body;
  }

  private OAuth2AccessTokenResponse requestToken(String tokenUri) {
    ClientRegistration registration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
        .clientId("client-id").clientSecret("client-secret").tokenUri(tokenUri).build();
    OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
        .clientId("client-id").redirectUri("http://localhost/callback").state("state").build();
    OAuth2AuthorizationResponse response = OAuth2AuthorizationResponse.success("code")
        .redirectUri("http://localhost/callback").state("state").build();

    RestClientAuthorizationCodeTokenResponseClient client =
        new RestClientAuthorizationCodeTokenResponseClient();
    client.setRestClient(
        SplibOauth2HttpClients.forTokenEndpoint(new SimpleClientHttpRequestFactory()));
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient = client;

    return tokenClient.getTokenResponse(new OAuth2AuthorizationCodeGrantRequest(registration,
        new OAuth2AuthorizationExchange(request, response)));
  }

  @Test
  @DisplayName("the token client reads the token response")
  void tokenResponse() throws Exception {
    respond(200, "{\"access_token\":\"token\",\"token_type\":\"Bearer\",\"expires_in\":3600}");

    assertThat(requestToken(uri).getAccessToken().getTokenValue()).isEqualTo("token");
  }

  @Test
  @DisplayName("the token client converts an error response to the OAuth2 error")
  void tokenError() throws Exception {
    respond(400, ERROR_BODY);

    assertThatThrownBy(() -> requestToken(uri)).isInstanceOf(OAuth2AuthorizationException.class)
        .satisfies(ex -> assertThat(((OAuth2AuthorizationException) ex).getError().getErrorCode())
            .isEqualTo("invalid_grant"));
  }

  @Test
  @DisplayName("the UserInfo client converts an error response to the OAuth2 error")
  void userInfoError() throws Exception {
    respond(400, ERROR_BODY);
    RestOperations client =
        SplibOauth2HttpClients.forUserInfoEndpoint(new SimpleClientHttpRequestFactory());

    assertThatThrownBy(() -> client.getForObject(uri, String.class))
        .isInstanceOf(OAuth2AuthorizationException.class)
        .satisfies(ex -> assertThat(((OAuth2AuthorizationException) ex).getError().getErrorCode())
            .isEqualTo("invalid_grant"));
  }
}
