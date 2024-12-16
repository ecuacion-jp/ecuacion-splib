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
package jp.ecuacion.splib.web.config;

import jakarta.servlet.SessionTrackingMode;
import java.util.Collections;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "jp.ecuacion.splib.core.config"
    + ",jp.ecuacion.splib.web.advice"
    + ",jp.ecuacion.splib.web.controller"
    + ",jp.ecuacion.splib.web.service"
    + ",jp.ecuacion.splib.web.util"
  )
public class SplibWebConfig {

  /**
   * 時々、URLが勝手に以下のように";" + jsessionIdが入り込み、";"が入ることでspring securityでエラーが発生しシステムエラーとなる。
   * 
   * <pre>
   * 問題のURL: https://aws-instance-manager.ecuacion.jp/aws-instance-manager/public/showPage/page;jsessionid=D51E9ED564B7148E81EE82C4E33EC70A?notFound=&page=home
   * 例外message： org.springframework.security.web.firewall.RequestRejectedException: 
   * The request was rejected because the URL contained a potentially malicious String ";"
   * </pre>
   * 
   * <p>
   * これを避けるため、以下の対応をとる。
   * https://code-kakikaki.com/springboot-url-jsessionid/
   * </p>
   */
  @Bean
  ServletContextInitializer servletContextInitializer() {
    ServletContextInitializer initializer = servletContext -> {
      servletContext.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.COOKIE));
    };

    return initializer;
  }
}
