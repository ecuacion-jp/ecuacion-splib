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

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Wraps {@link DefaultOAuth2AuthorizationRequestResolver} and adds
 *     {@code response_mode=form_post} to authorization requests sent to Apple.
 *
 * <p>Apple rejects authorization requests that ask for the {@code name} or {@code email} scope
 *     without {@code response_mode=form_post}.
 *     With {@code form_post}, Apple also posts the user's name ({@code user} parameter)
 *     on the first login.</p>
 *
 * <p>Apple is detected by the host of the authorization endpoint, not by the registrationId,
 *     so it works whatever registrationId the app uses.
 *     Only public API is used here: the registrationId stored in the request attributes
 *     is an internal detail of Spring Security and its key has changed in the past.</p>
 */
public class SplibOauth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

  private final OAuth2AuthorizationRequestResolver delegate;

  /**
   * Constructs a new instance.
   *
   * @param clientRegistrationRepository clientRegistrationRepository
   * @param authorizationRequestBaseUri the base URI used for authorization requests,
   *     like {@code /oauth2/authorization}
   */
  public SplibOauth2AuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository,
      String authorizationRequestBaseUri) {
    this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository,
        authorizationRequestBaseUri);
  }

  @Override
  public @Nullable OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    return customize(delegate.resolve(request));
  }

  @Override
  public @Nullable OAuth2AuthorizationRequest resolve(HttpServletRequest request,
      String clientRegistrationId) {
    return customize(delegate.resolve(request, clientRegistrationId));
  }

  private @Nullable OAuth2AuthorizationRequest customize(
      @Nullable OAuth2AuthorizationRequest authorizationRequest) {
    if (authorizationRequest == null
        || !SplibAppleEndpoints.isApple(authorizationRequest.getAuthorizationUri())) {
      return authorizationRequest;
    }

    // from() doesn't copy authorizationRequestUri, so build() rebuilds it
    // including the added parameter.
    return OAuth2AuthorizationRequest.from(authorizationRequest)
        .additionalParameters(params -> params.put("response_mode", "form_post")).build();
  }
}
