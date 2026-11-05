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
package jp.ecuacion.splib.core.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SplibApacheHttpRequestFactories")
class SplibApacheHttpRequestFactoriesTest {

  @ParameterizedTest
  @CsvSource({"https://a.example.com/,https://b.example.com/,true",
      "http://a.example.com/,https://b.example.com/,true",
      "http://a.example.com/,http://b.example.com/,true",
      "https://a.example.com/,http://b.example.com/,false"})
  @DisplayName("redirects are followed except from HTTPS to HTTP")
  void redirect(String from, String to, boolean expected) throws Exception {
    RedirectStrategy strategy = SplibApacheHttpRequestFactories.redirectStrategy();
    HttpRequest request = new BasicHttpRequest("GET", URI.create(from));
    BasicClassicHttpResponse response = new BasicClassicHttpResponse(302);
    response.addHeader("Location", to);

    assertThat(strategy.isRedirected(request, response, HttpClientContext.create()))
        .isEqualTo(expected);
  }
}
