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

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Generates the Apple client secret JWT required for Sign in with Apple.
 *
 * <p>Apple requires a signed ES256 JWT as the {@code client_secret} for every
 * token request. This service builds and signs that JWT from the properties
 * below.</p>
 *
 * <p>Required properties (activate this bean by setting
 * {@code ecuacion.oauth2.apple.team-id}):</p>
 * <pre>
 * ecuacion.oauth2.apple.team-id=&lt;Apple Developer Team ID&gt;
 * ecuacion.oauth2.apple.key-id=&lt;Key ID from Apple Developer Portal&gt;
 * ecuacion.oauth2.apple.client-id=&lt;Service ID registered with Apple&gt;
 * ecuacion.oauth2.apple.private-key=&lt;PKCS8 PEM private key (single line, no headers)&gt;
 * </pre>
 */
@Service
@ConditionalOnProperty(name = "ecuacion.oauth2.apple.team-id")
public class SplibAppleClientSecretService {

  /** Valid duration of the generated JWT: 6 months in milliseconds. */
  private static final long EXPIRY_MS = 6L * 30 * 24 * 60 * 60 * 1000;

  @Value("${ecuacion.oauth2.apple.team-id}")
  @SuppressWarnings("NullAway")
  private String teamId;

  @Value("${ecuacion.oauth2.apple.key-id}")
  @SuppressWarnings("NullAway")
  private String keyId;

  @Value("${ecuacion.oauth2.apple.client-id}")
  @SuppressWarnings("NullAway")
  private String clientId;

  /** PKCS8 PEM content without header/footer lines and whitespace. */
  @Value("${ecuacion.oauth2.apple.private-key}")
  @SuppressWarnings("NullAway")
  private String privateKeyPem;

  /**
   * Generates a signed ES256 JWT to use as the Apple {@code client_secret}.
   *
   * @return signed JWT string
   */
  public String generateClientSecret() {
    try {
      ECPrivateKey ecPrivateKey = parsePrivateKey(privateKeyPem);

      JWTClaimsSet claims = new JWTClaimsSet.Builder()
          .issuer(teamId)
          .issueTime(new Date())
          .expirationTime(new Date(System.currentTimeMillis() + EXPIRY_MS))
          .audience("https://appleid.apple.com")
          .subject(clientId)
          .build();

      JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build();
      SignedJWT jwt = new SignedJWT(header, claims);
      jwt.sign(new ECDSASigner(ecPrivateKey));
      return jwt.serialize();

    } catch (Exception ex) {
      throw new RuntimeException("Failed to generate Apple client secret JWT", ex);
    }
  }

  private ECPrivateKey parsePrivateKey(String pem) throws Exception {
    String stripped = pem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s+", "");
    byte[] keyBytes = Base64.getDecoder().decode(stripped);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
    return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(spec);
  }
}
