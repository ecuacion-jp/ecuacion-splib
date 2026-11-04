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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Tests {@link SplibOauth2AuthorizationRequestResolver} with the real
 * {@code DefaultOAuth2AuthorizationRequestResolver}, so that a change in Spring Security's
 * behavior is detected when its version is upgraded.
 */
@DisplayName("SplibOauth2AuthorizationRequestResolver")
class SplibOauth2AuthorizationRequestResolverTest {

  private static final String BASE_URI = "/oauth2/authorization";

  /** registrationId other than "apple" to confirm Apple is detected by the endpoint. */
  private static final String APPLE_REGISTRATION_ID = "sign-in-with-apple";

  private final SplibOauth2AuthorizationRequestResolver resolver =
      new SplibOauth2AuthorizationRequestResolver(
          new InMemoryClientRegistrationRepository(apple(), google()), BASE_URI);

  private static ClientRegistration apple() {
    return ClientRegistration.withRegistrationId(APPLE_REGISTRATION_ID).clientId("apple-client")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "name", "email")
        .authorizationUri("https://appleid.apple.com/auth/authorize")
        .tokenUri("https://appleid.apple.com/auth/token")
        .jwkSetUri("https://appleid.apple.com/auth/keys").userNameAttributeName("sub").build();
  }

  private static ClientRegistration google() {
    return CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("google-client")
        .clientSecret("google-secret").build();
  }

  private static MockHttpServletRequest request(String registrationId) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", BASE_URI + "/" + registrationId);
    request.setServletPath(BASE_URI + "/" + registrationId);
    return request;
  }

  @Test
  @DisplayName("resolve(request): adds response_mode=form_post for Apple")
  void apple_formPostAdded() {
    OAuth2AuthorizationRequest req =
        Objects.requireNonNull(resolver.resolve(request(APPLE_REGISTRATION_ID)));

    assertThat(req.getAdditionalParameters()).containsEntry("response_mode", "form_post");
    // The URL actually sent to Apple must contain it, not only additionalParameters.
    assertThat(req.getAuthorizationRequestUri()).contains("response_mode=form_post");
  }

  @Test
  @DisplayName("resolve(request, id): adds response_mode=form_post for Apple")
  void apple_formPostAdded_withRegistrationId() {
    OAuth2AuthorizationRequest req = Objects.requireNonNull(
        resolver.resolve(new MockHttpServletRequest("GET", "/any"), APPLE_REGISTRATION_ID));

    assertThat(req.getAdditionalParameters()).containsEntry("response_mode", "form_post");
    assertThat(req.getAuthorizationRequestUri()).contains("response_mode=form_post");
  }

  @Test
  @DisplayName("Apple: other parameters (state, nonce, scope) are kept")
  void apple_otherParametersKept() {
    OAuth2AuthorizationRequest req =
        Objects.requireNonNull(resolver.resolve(request(APPLE_REGISTRATION_ID)));

    assertThat(req.getState()).isNotBlank();
    assertThat(req.getAdditionalParameters()).containsKey("nonce");
    // state is URL-encoded in the URL ("=" -> "%3D").
    String encodedState =
        URLEncoder.encode(Objects.requireNonNull(req.getState()), StandardCharsets.UTF_8);
    assertThat(req.getAuthorizationRequestUri()).contains("state=" + encodedState)
        .contains("nonce=").contains("scope=openid%20name%20email");
  }

  @Test
  @DisplayName("Google: response_mode is not added")
  void google_formPostNotAdded() {
    OAuth2AuthorizationRequest req = Objects.requireNonNull(resolver.resolve(request("google")));

    assertThat(req.getAdditionalParameters()).doesNotContainKey("response_mode");
    assertThat(req.getAuthorizationRequestUri()).doesNotContain("response_mode");
  }

  @Test
  @DisplayName("non-authorization request: returns null")
  void nonAuthorizationRequest_null() {
    assertThat(resolver.resolve(new MockHttpServletRequest("GET", "/other"))).isNull();
  }
}
