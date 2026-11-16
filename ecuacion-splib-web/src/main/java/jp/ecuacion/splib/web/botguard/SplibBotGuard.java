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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import net.logicsquad.nanocaptcha.content.LatinContentProducer;
import net.logicsquad.nanocaptcha.image.ImageCaptcha;
import net.logicsquad.nanocaptcha.image.background.GradiatedBackgroundProducer;
import net.logicsquad.nanocaptcha.image.noise.CurvedLineNoiseProducer;
import net.logicsquad.nanocaptcha.image.renderer.DefaultWordRenderer;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/**
 * Provides the protection of forms open to anyone (an inquiry form, a sign-up form, etc.)
 *     against bots.
 *
 * <p>It's a bean registered by {@code SplibWebConfig}. Three checks are made, in this order, by
 *     {@link #check(HttpServletRequest, String, String)}:</p>
 *
 * <ol>
 * <li><strong>Honeypot</strong>: a text field people cannot see (the {@code inputHoneypot}
 *     component, whose parameter is {@link #HONEYPOT_PARAMETER}). Many bots fill in every field,
 *     so a value in it is regarded as a bot.</li>
 * <li><strong>Minimum time to submit</strong>: a form submitted less than
 *     {@code jp.ecuacion.splib.web.bot-guard.min-seconds-to-submit} seconds (default: 3) after it
 *     was displayed is regarded as a bot. A form submitted without being displayed in the session
 *     is also regarded as a bot.</li>
 * <li><strong>Image CAPTCHA</strong> (only when the form is prepared by
 *     {@link #prepareFormWithCaptcha}): 6 latin characters, case-insensitive. The answer is kept in
 *     the session and can be used only once, so a new image is needed after a wrong answer.</li>
 * </ol>
 *
 * <p>Apps call {@link #prepareForm} or {@link #prepareFormWithCaptcha} every time the form is
 *     displayed (including when it's displayed again after an input error), and
 *     {@link #check(HttpServletRequest, String, String)} when it's submitted, before saving
 *     anything. The state is kept in the session per {@code formName}, and removed by the
 *     check.</p>
 *
 * <p>When a bot is detected, apps are expected to show the same page as a success without saving
 *     or sending anything, so that the bot is not told it was detected. It's logged as a warning
 *     (without the values sent).</p>
 *
 * <p>The honeypot and the minimum time stop simple bots without bothering people. Bots which read
 *     the image (by OCR, AI or people paid to read it) can pass the CAPTCHA.</p>
 */
public class SplibBotGuard {

  /** The property of the minimum seconds from displaying the form to submitting it. */
  public static final String PROPERTY_MIN_SECONDS_TO_SUBMIT =
      "jp.ecuacion.splib.web.bot-guard.min-seconds-to-submit";

  /** The default of {@link #PROPERTY_MIN_SECONDS_TO_SUBMIT}. */
  public static final int DEFAULT_MIN_SECONDS_TO_SUBMIT = 3;

  /**
   * The request parameter of the honeypot field.
   *
   * <p>It's a name bots tend to fill in. It's not bound to the form, so apps don't need a field
   *     for it.</p>
   */
  public static final String HONEYPOT_PARAMETER = "website";

  /** The message key shown when the CAPTCHA answer is wrong. */
  public static final String MSG_CAPTCHA_WRONG = "jp.ecuacion.splib.web.botGuard.captchaWrong";

  /** The number of characters of the CAPTCHA. */
  static final int CAPTCHA_LENGTH = 6;

  private static final int CAPTCHA_WIDTH = 200;
  private static final int CAPTCHA_HEIGHT = 50;

  private static final String SESSION_KEY_PREFIX = "jp.ecuacion.splib.web.botGuard.";

  /** The result of {@link #check(HttpServletRequest, String, String)}. */
  public enum Result {
    /** Not regarded as a bot, and the CAPTCHA answer (if used) is right. */
    PASSED,
    /**
     * Regarded as a bot by the honeypot or the minimum time. Apps should show the same page as a
     *     success without saving or sending anything.
     */
    BOT_DETECTED,
    /**
     * The CAPTCHA answer is wrong. Apps should show the form again with an error (for example by
     *     {@link SplibBotGuard#throwCaptchaWrong(String)}). A new image is needed.
     */
    CAPTCHA_WRONG
  }

  private final DetailLogger detailLog = new DetailLogger(this);

  private final Duration minTimeToSubmit;
  private final Clock clock;

  /**
   * Constructs a new instance with {@code environment}.
   *
   * @param environment the environment to read the properties from
   */
  public SplibBotGuard(Environment environment) {
    this(environment, Clock.systemUTC());
  }

  SplibBotGuard(Environment environment, Clock clock) {
    this.minTimeToSubmit = Duration.ofSeconds(environment.getProperty(
        PROPERTY_MIN_SECONDS_TO_SUBMIT, Integer.class, DEFAULT_MIN_SECONDS_TO_SUBMIT));
    this.clock = clock;
  }

  /**
   * Records that the form is displayed, for a form without the CAPTCHA.
   *
   * @param request the request displaying the form
   * @param formName the name of the form, unique in the app (used for the session keys and the
   *     log)
   */
  public void prepareForm(HttpServletRequest request, String formName) {
    HttpSession session = request.getSession();
    session.setAttribute(sessionKey(formName, "displayedAt"), clock.instant());
    session.removeAttribute(sessionKey(formName, "captchaAnswer"));
  }

  /**
   * Records that the form is displayed, and creates a new CAPTCHA image.
   *
   * <p>The image replaces the one displayed before, so the answer to an old image cannot be
   *     used.</p>
   *
   * @param request the request displaying the form
   * @param formName the name of the form, unique in the app (used for the session keys and the
   *     log)
   * @return the image as a data URI ({@code data:image/png;base64,...}), to be put in the field
   *     the {@code inputCaptcha} component reads ({@code <itemPropertyPath>ImageData})
   */
  public String prepareFormWithCaptcha(HttpServletRequest request, String formName) {
    ImageCaptcha captcha = new ImageCaptcha.Factory.Builder(CAPTCHA_WIDTH, CAPTCHA_HEIGHT)
        .addContent(new LatinContentProducer(CAPTCHA_LENGTH),
            new DefaultWordRenderer.Builder().build())
        .addBackground(new GradiatedBackgroundProducer()).addNoise(new CurvedLineNoiseProducer())
        .build().create();

    prepareForm(request, formName);
    request.getSession().setAttribute(sessionKey(formName, "captchaAnswer"),
        captcha.getContent());
    return captcha.toDataUri();
  }

  /**
   * Checks the submitted form.
   *
   * <p>The state in the session (the time displayed and the CAPTCHA answer) is removed, so the
   *     form needs to be displayed again before the next submission.</p>
   *
   * @param request the request submitting the form
   * @param formName the name of the form given to {@link #prepareForm} or
   *     {@link #prepareFormWithCaptcha}
   * @param captchaInput the characters entered for the CAPTCHA, may be {@code null} when the form
   *     doesn't use the CAPTCHA
   * @return the result
   */
  public Result check(HttpServletRequest request, String formName,
      @Nullable String captchaInput) {
    HttpSession session = request.getSession();
    String displayedAtKey = sessionKey(formName, "displayedAt");
    String captchaAnswerKey = sessionKey(formName, "captchaAnswer");
    final Instant displayedAt = (Instant) session.getAttribute(displayedAtKey);
    final String captchaAnswer = (String) session.getAttribute(captchaAnswerKey);
    session.removeAttribute(displayedAtKey);
    session.removeAttribute(captchaAnswerKey);

    String botReason =
        getBotReason(request.getParameter(HONEYPOT_PARAMETER), displayedAt, clock.instant());
    if (botReason != null) {
      detailLog.warn("Form submission ignored as a bot (form: " + formName + "): " + botReason);
      return Result.BOT_DETECTED;
    }

    if (captchaAnswer != null) {
      String input = captchaInput == null ? "" : captchaInput.strip();
      if (!captchaAnswer.equalsIgnoreCase(input)) {
        return Result.CAPTCHA_WRONG;
      }
    }

    return Result.PASSED;
  }

  /**
   * Throws the violation of the wrong CAPTCHA answer, shown on the CAPTCHA field.
   *
   * @param captchaItemPropertyPath the item property path of the CAPTCHA field (the one given to
   *     the {@code inputCaptcha} component, like {@code captcha})
   */
  public static void throwCaptchaWrong(String captchaItemPropertyPath) {
    new Violations().add(
        new BusinessViolation(new String[] {captchaItemPropertyPath}, MSG_CAPTCHA_WRONG))
        .throwIfAny();
  }

  /**
   * Returns the reason to regard the submission as a bot, {@code null} if it's not.
   *
   * @param honeypot the value of the honeypot field, may be {@code null}
   * @param displayedAt the time the form was displayed, may be {@code null} when not displayed in
   *     the session
   * @param now the time submitted
   * @return the reason (for the log, without the values sent), {@code null} if not a bot
   */
  @Nullable
  String getBotReason(@Nullable String honeypot, @Nullable Instant displayedAt, Instant now) {
    if (honeypot != null && !honeypot.isEmpty()) {
      return "the honeypot field is filled.";
    }

    if (displayedAt == null) {
      return "the form has not been displayed in the session.";
    }

    Duration elapsed = Duration.between(displayedAt, now);
    if (elapsed.compareTo(minTimeToSubmit) < 0) {
      return "submitted " + elapsed.toMillis() + " ms after the form was displayed.";
    }

    return null;
  }

  private static String sessionKey(String formName, String name) {
    return SESSION_KEY_PREFIX + formName + "." + name;
  }
}
