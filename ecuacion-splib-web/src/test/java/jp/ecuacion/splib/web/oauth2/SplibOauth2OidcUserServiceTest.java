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
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

@DisplayName("SplibOauth2OidcUserService")
class SplibOauth2OidcUserServiceTest {

  /** Without the UserInfo endpoint, the user is made from the ID token without communication. */
  private static final ClientRegistration REGISTRATION =
      CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("id").clientSecret("secret")
          .userInfoUri(null).build();

  private static OidcUserRequest request(Map<String, Object> claims) {
    OidcIdToken.Builder builder = OidcIdToken.withTokenValue("id-token").claim("sub", "sub1");
    claims.forEach(builder::claim);
    OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
        "access-token", Instant.now(), Instant.now().plusSeconds(60));
    return new OidcUserRequest(REGISTRATION, accessToken, builder.build());
  }

  static Stream<@NonNull Arguments> verifiedValues() {
    // Apple may send it as a string
    return Stream.of(Arguments.of(true), Arguments.of("true"));
  }

  @ParameterizedTest(name = "email_verified: {0}")
  @MethodSource("verifiedValues")
  @DisplayName("returns the user when the email is verified")
  void verified(Object emailVerified) {
    assertThat(new SplibOauth2OidcUserService()
        .loadUser(request(Map.of("email", "a@example.com", "email_verified", emailVerified)))
        .getEmail()).isEqualTo("a@example.com");
  }

  static Stream<@NonNull Arguments> notVerifiedClaims() {
    return Stream.of(Arguments.of("false", Map.of("email", "a@example.com",
        "email_verified", false)),
        Arguments.of("not set", Map.of("email", "a@example.com")));
  }

  @ParameterizedTest(name = "email_verified: {0}")
  @MethodSource("notVerifiedClaims")
  @DisplayName("fails with email_not_verified when the email is not verified")
  void notVerified(String label, Map<String, Object> claims) {
    assertThatThrownBy(() -> new SplibOauth2OidcUserService().loadUser(request(claims)))
        .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
            ex -> assertThat(ex.getError().getErrorCode())
                .isEqualTo(SplibOauth2OidcUserService.EMAIL_NOT_VERIFIED_ERROR_CODE));
  }

  @Test
  @DisplayName("fails with email_not_provided when the email is not provided")
  void notProvided() {
    assertThatThrownBy(() -> new SplibOauth2OidcUserService()
        .loadUser(request(Map.of("email_verified", true))))
        .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
            ex -> assertThat(ex.getError().getErrorCode())
                .isEqualTo(SplibOauth2OidcUserService.EMAIL_NOT_PROVIDED_ERROR_CODE));
  }
}
