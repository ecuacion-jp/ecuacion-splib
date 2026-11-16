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
package jp.ecuacion.splib.web.botguard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.splib.web.adminlogin.AdminLoginTestSupport.MutableClock;
import jp.ecuacion.splib.web.botguard.SplibBotGuard.Result;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/** Tests the honeypot, the minimum time to submit and the CAPTCHA of public forms. */
@DisplayName("SplibBotGuard")
class SplibBotGuardTest {

  private static final String FORM = "inquiry";

  private final MutableClock clock = new MutableClock();
  private final MockHttpSession session = new MockHttpSession();
  private SplibBotGuard guard = new SplibBotGuard(new MockEnvironment(), clock);

  private MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(session);
    return request;
  }

  private String captchaAnswer() {
    return (String) Objects.requireNonNull(
        session.getAttribute("jp.ecuacion.splib.web.botGuard." + FORM + ".captchaAnswer"));
  }

  private Result submit(@Nullable String captchaInput) {
    return guard.check(request(), FORM, captchaInput);
  }

  private Result submitWithHoneypot(String honeypot, @Nullable String captchaInput) {
    MockHttpServletRequest request = request();
    request.setParameter(SplibBotGuard.HONEYPOT_PARAMETER, honeypot);
    return guard.check(request, FORM, captchaInput);
  }

  @Nested
  @DisplayName("CAPTCHA")
  class Captcha {

    @Test
    @DisplayName("prepareFormWithCaptcha returns a PNG data URI and keeps a 6-character answer")
    void prepare() {
      String dataUri = guard.prepareFormWithCaptcha(request(), FORM);

      assertThat(dataUri).startsWith("data:image/png;base64,");
      assertThat(captchaAnswer()).hasSize(SplibBotGuard.CAPTCHA_LENGTH);
    }

    @Test
    @DisplayName("the right answer passes, ignoring the case and the surrounding spaces")
    void rightAnswer() {
      guard.prepareFormWithCaptcha(request(), FORM);
      String answer = captchaAnswer();
      clock.advance(Duration.ofSeconds(10));

      assertThat(submit(" " + answer.toUpperCase() + " ")).isEqualTo(Result.PASSED);
    }

    @Test
    @DisplayName("a wrong or missing answer is CAPTCHA_WRONG")
    void wrongAnswer() {
      guard.prepareFormWithCaptcha(request(), FORM);
      clock.advance(Duration.ofSeconds(10));
      assertThat(submit("wrong!")).isEqualTo(Result.CAPTCHA_WRONG);

      guard.prepareFormWithCaptcha(request(), FORM);
      clock.advance(Duration.ofSeconds(10));
      assertThat(submit(null)).isEqualTo(Result.CAPTCHA_WRONG);
    }

    @Test
    @DisplayName("the answer can be used only once: submitting again without displaying is a bot")
    void usedOnlyOnce() {
      guard.prepareFormWithCaptcha(request(), FORM);
      String answer = captchaAnswer();
      clock.advance(Duration.ofSeconds(10));
      submit("wrong!");

      assertThat(submit(answer)).isEqualTo(Result.BOT_DETECTED);
    }

    @Test
    @DisplayName("the answer to an old image cannot be used after a new image is created")
    void oldImage() {
      guard.prepareFormWithCaptcha(request(), FORM);
      String oldAnswer = captchaAnswer();
      guard.prepareFormWithCaptcha(request(), FORM);
      clock.advance(Duration.ofSeconds(10));

      // the 2 answers are random, so they are the same only by a very small chance
      if (!oldAnswer.equalsIgnoreCase(captchaAnswer())) {
        assertThat(submit(oldAnswer)).isEqualTo(Result.CAPTCHA_WRONG);
      }
    }

    @Test
    @DisplayName("prepareForm (without CAPTCHA) after a CAPTCHA removes the answer")
    void prepareFormRemovesAnswer() {
      guard.prepareFormWithCaptcha(request(), FORM);
      guard.prepareForm(request(), FORM);
      clock.advance(Duration.ofSeconds(10));

      assertThat(submit(null)).isEqualTo(Result.PASSED);
    }

    @Test
    @DisplayName("throwCaptchaWrong throws the violation of the CAPTCHA field")
    void throwCaptchaWrong() {
      assertThatThrownBy(() -> SplibBotGuard.throwCaptchaWrong("captcha"))
          .isInstanceOf(ViolationException.class);
    }
  }

  @Nested
  @DisplayName("honeypot and minimum time")
  class BotDetection {

    @Test
    @DisplayName("a form without CAPTCHA passes after the minimum time with the honeypot empty")
    void passes() {
      guard.prepareForm(request(), FORM);
      clock.advance(Duration.ofSeconds(SplibBotGuard.DEFAULT_MIN_SECONDS_TO_SUBMIT));

      assertThat(submitWithHoneypot("", null)).isEqualTo(Result.PASSED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com", " "})
    @DisplayName("a value in the honeypot is a bot, before checking the CAPTCHA")
    void honeypot(String honeypot) {
      guard.prepareFormWithCaptcha(request(), FORM);
      String answer = captchaAnswer();
      clock.advance(Duration.ofSeconds(10));

      assertThat(submitWithHoneypot(honeypot, answer)).isEqualTo(Result.BOT_DETECTED);
    }

    @Test
    @DisplayName("submitting before the minimum time is a bot, even with the right answer")
    void tooFast() {
      guard.prepareFormWithCaptcha(request(), FORM);
      String answer = captchaAnswer();
      clock.advance(Duration.ofMillis(2999));

      assertThat(submit(answer)).isEqualTo(Result.BOT_DETECTED);
    }

    @Test
    @DisplayName("submitting a form not displayed in the session is a bot")
    void notDisplayed() {
      assertThat(submit(null)).isEqualTo(Result.BOT_DETECTED);
    }

    @Test
    @DisplayName("forms are kept separately by the form name")
    void formsAreSeparate() {
      guard.prepareForm(request(), "other");
      clock.advance(Duration.ofSeconds(10));

      assertThat(submit(null)).isEqualTo(Result.BOT_DETECTED);
      assertThat(guard.check(request(), "other", null)).isEqualTo(Result.PASSED);
    }

    @Test
    @DisplayName("the minimum time can be changed by the property")
    void property() {
      guard = new SplibBotGuard(new MockEnvironment()
          .withProperty(SplibBotGuard.PROPERTY_MIN_SECONDS_TO_SUBMIT, "10"), clock);
      guard.prepareForm(request(), FORM);
      clock.advance(Duration.ofSeconds(9));
      assertThat(submit(null)).isEqualTo(Result.BOT_DETECTED);

      guard.prepareForm(request(), FORM);
      clock.advance(Duration.ofSeconds(10));
      assertThat(submit(null)).isEqualTo(Result.PASSED);
    }
  }
}
