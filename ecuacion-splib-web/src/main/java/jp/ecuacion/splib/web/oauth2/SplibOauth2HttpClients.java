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

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Creates the HTTP clients of OAuth2 login with the given request factory.
 *
 * <p>Spring Security's clients can't change only the request factory, so these repeat
 *     the other settings of its default clients.</p>
 */
public final class SplibOauth2HttpClients {

  private SplibOauth2HttpClients() {}

  /**
   * Creates the client for the token endpoint.
   *
   * @param requestFactory requestFactory
   * @return RestClient
   */
  public static RestClient forTokenEndpoint(ClientHttpRequestFactory requestFactory) {
    return RestClient.builder().requestFactory(requestFactory)
        .configureMessageConverters(converters -> {
          converters.addCustomConverter(new FormHttpMessageConverter());
          converters.addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter());
        }).defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build();
  }

  /**
   * Creates the client for the UserInfo endpoint.
   *
   * @param requestFactory requestFactory
   * @return RestOperations
   */
  public static RestOperations forUserInfoEndpoint(ClientHttpRequestFactory requestFactory) {
    RestTemplate restTemplate = new RestTemplate(requestFactory);
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
    return restTemplate;
  }
}
