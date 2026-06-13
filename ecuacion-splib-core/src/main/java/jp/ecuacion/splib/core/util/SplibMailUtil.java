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
package jp.ecuacion.splib.core.util;

import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.MailUtil;
import jp.ecuacion.lib.core.util.MailUtil.MailUtilConfig;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends mail using Spring Boot standard {@code spring.mail.*} properties.
 *
 * <p>SMTP connection settings are read from {@code spring.mail.*}.
 *     Application-level settings (subject prefix, error notification addresses, etc.)
 *     are read from {@code jp.ecuacion.splib.mail.*}.</p>
 *
 * <p>If {@code spring.mail.host} or
 *     {@code jp.ecuacion.splib.mail.address-csv-on-system-error} is not set,
 *     {@link #sendErrorMail} returns silently without sending.</p>
 */
@Component
public class SplibMailUtil {

  DetailLogger detailLog = new DetailLogger(this);

  @Value("${spring.mail.host:#{null}}")
  private @Nullable String host;

  @Value("${spring.mail.port:587}")
  private int port;

  @Value("${spring.mail.username:#{null}}")
  private @Nullable String username;

  @Value("${spring.mail.password:#{null}}")
  private @Nullable String password;

  @Value("${spring.mail.properties.mail.smtp.auth:true}")
  private boolean auth;

  @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
  private boolean sslEnable;

  @SuppressWarnings("null")
  @Value("${jp.ecuacion.splib.mail.title-prefix:}")
  private String titlePrefix;

  @Value("${jp.ecuacion.splib.mail.address-csv-on-system-error:#{null}}")
  private @Nullable String errorAddressCsv;

  @Value("${jp.ecuacion.splib.mail.smtp.bounce-address:#{null}}")
  private @Nullable String bounceAddress;

  @Value("${jp.ecuacion.splib.mail.smtp.checks-certificate:true}")
  private boolean checksCertificate;

  @Value("${jp.ecuacion.splib.mail.debug:false}")
  private boolean debug;

  /**
   * Sends an error notification mail.
   *
   * <p>If {@code spring.mail.host}, {@code spring.mail.username},
   *     {@code spring.mail.password}, or
   *     {@code jp.ecuacion.splib.mail.address-csv-on-system-error} is not configured,
   *     logs a message and returns without sending.</p>
   *
   * @param th the throwable that caused the error
   */
  public void sendErrorMail(Throwable th) {
    if (host == null || username == null || password == null || errorAddressCsv == null) {
      detailLog.info("A system error occured but no mails sent since mail settings not exist.");
      return;
    }

    detailLog.info("Send a mail to notice the occurence of a system error to administrators.");
    MailUtilConfig config = new MailUtilConfig(Objects.requireNonNull(host), port, sslEnable, auth,
        checksCertificate, Objects.requireNonNull(username), Objects.requireNonNull(password),
        bounceAddress, debug, titlePrefix, Objects.requireNonNull(errorAddressCsv));
    MailUtil.sendErrorMail(th, config);
  }
}
