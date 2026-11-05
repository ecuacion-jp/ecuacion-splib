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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@DisplayName("SplibOauth2JwtDecoderFactory")
class SplibOauth2JwtDecoderFactoryTest {

  private static final String ISSUER = "https://accounts.google.com";
  private static final String CLIENT_ID = "client-id";

  @SuppressWarnings("null")
  private static RSAKey rsaKey;

  private final ClientRegistration registration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
      .clientId(CLIENT_ID).clientSecret("client-secret").build();

  @SuppressWarnings("null")
  private JwtDecoder decoder;
  @SuppressWarnings("null")
  private MockRestServiceServer jwkServer;

  @BeforeAll
  static void beforeAll() throws Exception {
    rsaKey = new RSAKeyGenerator(2048).keyID("key").generate();
  }

  @BeforeEach
  void before() {
    RestTemplate restTemplate = new RestTemplate();
    jwkServer = MockRestServiceServer.createServer(restTemplate);
    jwkServer.expect(ExpectedCount.manyTimes(),
        requestTo(Objects.requireNonNull(registration.getProviderDetails().getJwkSetUri())))
        .andRespond(withSuccess(new JWKSet(rsaKey.toPublicJWK()).toString(),
            MediaType.APPLICATION_JSON));
    decoder = new SplibOauth2JwtDecoderFactory(restTemplate).createDecoder(registration);
  }

  private JWTClaimsSet.Builder validClaims() {
    Instant now = Instant.now();
    return new JWTClaimsSet.Builder().issuer(ISSUER).subject("subject").audience(CLIENT_ID)
        .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300)));
  }

  private String sign(JWTClaimsSet claims) throws Exception {
    SignedJWT jwt =
        new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("key").build(), claims);
    jwt.sign(new RSASSASigner(rsaKey));
    return jwt.serialize();
  }

  @Test
  @DisplayName("a valid token is decoded with the keys fetched by the given client")
  void valid() throws Exception {
    Jwt jwt = decoder.decode(sign(validClaims().build()));

    assertThat(jwt.getSubject()).isEqualTo("subject");
    jwkServer.verify();
  }

  @Test
  @DisplayName("claims are converted same as OidcIdTokenDecoderFactory")
  void claimsConverted() throws Exception {
    Jwt jwt = decoder.decode(sign(validClaims().build()));

    assertThat((Object) jwt.getClaim("iss")).isInstanceOf(URL.class);
  }

  @Test
  @DisplayName("a token of another issuer is rejected")
  void wrongIssuer() throws Exception {
    String token = sign(validClaims().issuer("https://evil.example.com").build());

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
  }

  @Test
  @DisplayName("a token for another client is rejected")
  void wrongAudience() throws Exception {
    String token = sign(validClaims().audience("another-client").build());

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
  }

  @Test
  @DisplayName("an expired token is rejected")
  void expired() throws Exception {
    Instant past = Instant.now().minusSeconds(3600);
    String token = sign(validClaims().issueTime(Date.from(past.minusSeconds(300)))
        .expirationTime(Date.from(past)).build());

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
  }

  @Test
  @DisplayName("a token signed with another key is rejected")
  void wrongKey() throws Exception {
    SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("key").build(),
        validClaims().build());
    jwt.sign(new RSASSASigner(new RSAKeyGenerator(2048).keyID("key").generate()));
    String token = jwt.serialize();

    assertThatThrownBy(() -> decoder.decode(token))
        .isInstanceOf(BadJwtException.class);
  }

  @Test
  @DisplayName("a registration without the JWK set URI is rejected")
  void noJwkSetUri() {
    ClientRegistration noJwk = CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId(CLIENT_ID)
        .clientSecret("client-secret").jwkSetUri(null).build();
    SplibOauth2JwtDecoderFactory factory = new SplibOauth2JwtDecoderFactory(new RestTemplate());

    assertThatThrownBy(() -> factory.createDecoder(noJwk))
        .isInstanceOf(OAuth2AuthenticationException.class);
  }
}
