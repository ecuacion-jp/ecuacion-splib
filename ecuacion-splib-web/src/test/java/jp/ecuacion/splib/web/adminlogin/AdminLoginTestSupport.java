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
package jp.ecuacion.splib.web.adminlogin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.ecuacion.splib.core.util.SplibMailUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Provides classes used by the tests of the admin login protection.
 */
public final class AdminLoginTestSupport {

  private AdminLoginTestSupport() {}

  /** A clock whose time is moved by the tests. */
  public static class MutableClock extends Clock {

    private Instant now = Instant.parse("2026-11-08T00:00:00Z");

    /** Moves the time. */
    public void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(@Nullable ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  /** A mail util which keeps the mails instead of sending them. */
  public static class CapturingMailUtil extends SplibMailUtil {

    private static final Pattern CODE = Pattern.compile("\\d{6}");

    /** The addresses of the mails sent. */
    public final List<String> addresses = new ArrayList<>();

    /** The bodies of the mails sent. */
    public final List<String> bodies = new ArrayList<>();

    /** Fails to send when {@code true}. */
    public boolean fails;

    @Override
    public void sendTextMail(@Nullable List<@NonNull String> mailToList,
        @Nullable List<@NonNull String> mailCcList, String title, String content) {
      if (fails) {
        throw new IllegalStateException("Failed to send (test).");
      }

      addresses.addAll(mailToList == null ? List.of() : mailToList);
      bodies.add(content);
    }

    /** Returns the code in the last mail. */
    public String lastCode() {
      Matcher matcher = CODE.matcher(bodies.get(bodies.size() - 1));
      if (!matcher.find()) {
        throw new IllegalStateException("No code in the mail.");
      }

      return matcher.group();
    }
  }
}
