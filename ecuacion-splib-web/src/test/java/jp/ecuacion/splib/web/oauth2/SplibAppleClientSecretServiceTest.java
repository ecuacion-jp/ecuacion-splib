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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SplibAppleClientSecretService")
class SplibAppleClientSecretServiceTest {

  @Test
  @DisplayName("generates a signed JWT with Apple's claims and a short expiry")
  void generateClientSecret() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(new ECGenParameterSpec("secp256r1"));
    KeyPair keyPair = generator.generateKeyPair();

    SplibAppleClientSecretService service = new SplibAppleClientSecretService();
    ReflectionTestUtils.setField(service, "teamId", "TEAM");
    ReflectionTestUtils.setField(service, "keyId", "KEY");
    ReflectionTestUtils.setField(service, "clientId", "jp.example.web");
    ReflectionTestUtils.setField(service, "privateKeyPem",
        Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));

    SignedJWT jwt = SignedJWT.parse(service.generateClientSecret());
    assertThat(jwt.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic()))).isTrue();
    assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
    assertThat(jwt.getHeader().getKeyID()).isEqualTo("KEY");

    JWTClaimsSet claims = jwt.getJWTClaimsSet();
    assertThat(claims.getIssuer()).isEqualTo("TEAM");
    assertThat(claims.getSubject()).isEqualTo("jp.example.web");
    assertThat(claims.getAudience()).containsExactly("https://appleid.apple.com");

    // exp and iat are in seconds. Read from the raw payload, since JWTClaimsSet converts them.
    Map<String, Object> payload = jwt.getPayload().toJSONObject();
    long validSec = ((Number) Objects.requireNonNull(payload.get("exp"))).longValue()
        - ((Number) Objects.requireNonNull(payload.get("iat"))).longValue();
    assertThat(validSec * 1000).isEqualTo(SplibAppleClientSecretService.EXPIRY_MS);
  }
}
