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

import java.util.List;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

/**
 * Creates ID token decoders that fetch the signing keys with the given client.
 *
 * <p>{@link OidcIdTokenDecoderFactory} is final and has no way to pass a client,
 *     so this repeats what it does for RS256, the only algorithm it uses by default
 *     and the one Google and Apple use. In particular {@link OidcIdTokenValidator}
 *     must be set: {@link NimbusJwtDecoder} doesn't validate the issuer or the audience
 *     by default, and the login still succeeds without them.</p>
 */
public class SplibOauth2JwtDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

  private static final String MISSING_SIGNATURE_VERIFIER_ERROR_CODE = "missing_signature_verifier";

  private final RestOperations restOperations;

  /**
   * Constructs a new instance with {@code restOperations}.
   *
   * @param restOperations the client to fetch the signing keys
   */
  public SplibOauth2JwtDecoderFactory(RestOperations restOperations) {
    this.restOperations = restOperations;
  }

  @Override
  public JwtDecoder createDecoder(
      @SuppressWarnings("null") ClientRegistration clientRegistration) {
    String jwkSetUri = clientRegistration.getProviderDetails().getJwkSetUri();
    if (jwkSetUri == null || !StringUtils.hasText(jwkSetUri)) {
      OAuth2Error error = new OAuth2Error(MISSING_SIGNATURE_VERIFIER_ERROR_CODE,
          "Failed to find a Signature Verifier for Client Registration: '"
              + clientRegistration.getRegistrationId()
              + "'. Check to ensure you have configured the JwkSet URI.",
          null);
      throw new OAuth2AuthenticationException(error, error.toString());
    }

    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
        .jwsAlgorithm(SignatureAlgorithm.RS256).restOperations(restOperations).build();
    decoder.setJwtValidator(JwtValidators
        .createDefaultWithValidators(List.of(new OidcIdTokenValidator(clientRegistration))));
    decoder.setClaimSetConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter());
    return decoder;
  }
}
