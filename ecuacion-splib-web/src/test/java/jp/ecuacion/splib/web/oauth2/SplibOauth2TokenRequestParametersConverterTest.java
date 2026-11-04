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

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.util.MultiValueMap;

/**
 * Tests {@link SplibOauth2TokenRequestParametersConverter} with the real
 * {@code DefaultOAuth2TokenRequestParametersConverter}, so that a change in Spring Security's
 * behavior is detected when its version is upgraded.
 */
@DisplayName("SplibOauth2TokenRequestParametersConverter")
class SplibOauth2TokenRequestParametersConverterTest {

  private static final String JWT = "dummy.apple.jwt";

  /** registrationId other than "apple" to confirm Apple is detected by the endpoint. */
  private static final String APPLE_REGISTRATION_ID = "sign-in-with-apple";

  private static final SplibAppleClientSecretService APPLE_SERVICE =
      new SplibAppleClientSecretService() {
        @Override
        public String generateClientSecret() {
          return JWT;
        }
      };

  private final SplibOauth2TokenRequestParametersConverter converter =
      new SplibOauth2TokenRequestParametersConverter(APPLE_SERVICE);

  private static ClientRegistration apple(ClientAuthenticationMethod method) {
    return ClientRegistration.withRegistrationId(APPLE_REGISTRATION_ID).clientId("apple-client")
        .clientAuthenticationMethod(method)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "name", "email")
        .authorizationUri("https://appleid.apple.com/auth/authorize")
        .tokenUri("https://appleid.apple.com/auth/token")
        .jwkSetUri("https://appleid.apple.com/auth/keys").userNameAttributeName("sub").build();
  }

  private static ClientRegistration google() {
    return CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("google-client")
        .clientSecret("google-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST).build();
  }

  private static OAuth2AuthorizationCodeGrantRequest grantRequest(ClientRegistration reg) {
    String redirectUri = "https://example.com/login/oauth2/code/" + reg.getRegistrationId();
    OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("https://example.com/authorize")
        .clientId(reg.getClientId()).redirectUri(redirectUri).state("state").build();
    OAuth2AuthorizationResponse authResponse = OAuth2AuthorizationResponse.success("code")
        .redirectUri(redirectUri).state("state").build();
    return new OAuth2AuthorizationCodeGrantRequest(reg,
        new OAuth2AuthorizationExchange(authRequest, authResponse));
  }

  @Test
  @DisplayName("Apple (client_secret_post): client_secret is exactly one, and it is the JWT")
  void apple_clientSecretPost_replaced() {
    MultiValueMap<String, String> params =
        converter.convert(grantRequest(apple(ClientAuthenticationMethod.CLIENT_SECRET_POST)));

    assertThat(params.get("client_secret")).containsExactly(JWT);
    assertThat(params.get("client_id")).containsExactly("apple-client");
    assertThat(params.getFirst("code")).isEqualTo("code");
  }

  @Test
  @DisplayName("Apple (none): client_secret is exactly one, and it is the JWT")
  void apple_none_replaced() {
    MultiValueMap<String, String> params =
        converter.convert(grantRequest(apple(ClientAuthenticationMethod.NONE)));

    assertThat(params.get("client_secret")).containsExactly(JWT);
  }

  @Test
  @DisplayName("Google: client_secret is kept as configured")
  void google_notReplaced() {
    MultiValueMap<String, String> params = converter.convert(grantRequest(google()));

    assertThat(params.get("client_secret")).containsExactly("google-secret");
  }

  @Test
  @DisplayName("Apple service not configured: default parameters are returned as is")
  void appleServiceNull_notReplaced() {
    MultiValueMap<String, String> params = new SplibOauth2TokenRequestParametersConverter(null)
        .convert(grantRequest(apple(ClientAuthenticationMethod.CLIENT_SECRET_POST)));

    assertThat(params.getOrDefault("client_secret", List.of())).doesNotContain(JWT);
  }
}
