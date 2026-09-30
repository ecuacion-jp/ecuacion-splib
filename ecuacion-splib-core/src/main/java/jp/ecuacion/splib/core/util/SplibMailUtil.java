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

import java.util.List;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.MailUtil;
import jp.ecuacion.lib.core.util.MailUtil.MailUtilConfig;
import org.jspecify.annotations.NonNull;
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

  /**
   * When {@code sslEnable} is {@code false} (STARTTLS on port 587), {@code true} fails the
   * connection rather than falling back to plaintext when the server doesn't support
   * STARTTLS. Setting this to {@code false} is a security risk: it lets SMTP authentication
   * (including the password) happen over an unencrypted connection whenever the server
   * lacks STARTTLS support. Only set it to {@code false} for a server known not to support
   * STARTTLS (e.g. a local test relay), never in production.
   */
  @Value("${jp.ecuacion.splib.mail.smtp.starttls-required:true}")
  private boolean starttlsRequired;

  /**
   * Prefix prepended to the mail subject.
   *
   * <p>Spring Boot reads {@code application.properties} as ISO-8859-1, so write this value in
   *     {@code application.yml} (read as UTF-8) when it contains non-ASCII (e.g. Japanese)
   *     characters.</p>
   */
  @Value("${jp.ecuacion.splib.mail.title-prefix:}")
  private String titlePrefix = "";

  @Value("${jp.ecuacion.splib.mail.address-csv-on-system-error:#{null}}")
  private @Nullable String errorAddressCsv;

  @Value("${jp.ecuacion.splib.mail.smtp.bounce-address:#{null}}")
  private @Nullable String bounceAddress;

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
    sendErrorMail(th, null);
  }

  /**
   * Sends an error notification mail adding an additional message to it.
   *
   * <p>If {@code spring.mail.host}, {@code spring.mail.username},
   *     {@code spring.mail.password}, or
   *     {@code jp.ecuacion.splib.mail.address-csv-on-system-error} is not configured,
   *     logs a message and returns without sending.</p>
   *
   * @param th the throwable that caused the error
   * @param additionalMessage additional message,
   *     may be {@code null} if no {@code additionalMessage} is needed.
   * @see MailUtil#sendErrorMail(Throwable, String, MailUtilConfig)
   */
  public void sendErrorMail(Throwable th, @Nullable String additionalMessage) {
    if (!hasServerSettings() || errorAddressCsv == null) {
      detailLog.warn("A system error occured but no mails sent since mail settings not exist.");
      return;
    }

    detailLog.info("Send a mail to notice the occurence of a system error to administrators.");
    MailUtil.sendErrorMail(th, additionalMessage, getConfig());
  }

  /**
   * Sends a warn mail.
   *
   * <p>If {@code spring.mail.host}, {@code spring.mail.username}
   *     or {@code spring.mail.password} is not configured,
   *     logs a message and returns without sending.</p>
   *
   * @param content content, may be {@code null} if no mailbody content needed.
   * @param mailToList list of mailadresses used for "TO" address
   * @see MailUtil#sendWarnMail(String, List, MailUtilConfig)
   */
  public void sendWarnMail(String content, List<@NonNull String> mailToList) {
    if (!hasServerSettings()) {
      detailLog.warn("A system warning occured but no mails sent since mail settings not exist.");
      return;
    }

    detailLog.warn("Send a mail to notice the occurence of a system warning to administrators.");
    MailUtil.sendWarnMail(content, mailToList, getConfig());
  }

  /**
   * Sends a text-format mail.
   *
   * @param mailToList mailToList.
   *     Either mailToList or mailCcList need to have at least one element.
   * @param mailCcList mailCcList.
   *     Either mailToList or mailCcList need to have at least one element.
   * @param title title
   * @param content content
   * @throws IllegalStateException when {@code spring.mail.host}, {@code spring.mail.username}
   *     or {@code spring.mail.password} is not configured
   * @throws Exception Exception
   * @see MailUtil#sendTextMail(List, List, String, String, MailUtilConfig)
   */
  public void sendTextMail(@Nullable List<@NonNull String> mailToList,
      @Nullable List<@NonNull String> mailCcList, String title, String content)
      throws Exception {
    MailUtil.sendTextMail(mailToList, mailCcList, title, content, getRequiredConfig());
  }

  /**
   * Sends a html-format mail.
   *
   * @param mailToList mailToList.
   *     Either mailToList or mailCcList need to have at least one element.
   * @param mailCcList mailCcList.
   *     Either mailToList or mailCcList need to have at least one element.
   * @param title title
   * @param content content
   * @throws IllegalStateException when {@code spring.mail.host}, {@code spring.mail.username}
   *     or {@code spring.mail.password} is not configured
   * @throws Exception Exception
   * @see MailUtil#sendHtmlMail(List, List, String, String, MailUtilConfig)
   */
  public void sendHtmlMail(@Nullable List<@NonNull String> mailToList,
      @Nullable List<@NonNull String> mailCcList, String title, String content)
      throws Exception {
    MailUtil.sendHtmlMail(mailToList, mailCcList, title, content, getRequiredConfig());
  }

  private boolean hasServerSettings() {
    return host != null && username != null && password != null;
  }

  private MailUtilConfig getRequiredConfig() {
    if (!hasServerSettings()) {
      throw new IllegalStateException("Mail settings (spring.mail.host, spring.mail.username, "
          + "spring.mail.password) are not configured.");
    }

    return getConfig();
  }

  private MailUtilConfig getConfig() {
    // errorAddressCsv is used only by sendErrorMail, which checks its existence beforehand.
    return new MailUtilConfig(Objects.requireNonNull(host), port, sslEnable, auth,
        starttlsRequired, Objects.requireNonNull(username), Objects.requireNonNull(password),
        bounceAddress, debug, titlePrefix, Objects.requireNonNullElse(errorAddressCsv, ""));
  }
}
