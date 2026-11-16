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

import net.logicsquad.nanocaptcha.content.LatinContentProducer;
import net.logicsquad.nanocaptcha.image.ImageCaptcha;
import net.logicsquad.nanocaptcha.image.background.GradiatedBackgroundProducer;
import net.logicsquad.nanocaptcha.image.noise.CurvedLineNoiseProducer;
import net.logicsquad.nanocaptcha.image.renderer.DefaultWordRenderer;

/**
 * Creates the image of the CAPTCHA with nanocaptcha.
 *
 * <p>nanocaptcha is an optional dependency of ecuacion-splib-web, so all the references to it are
 *     kept in this class. {@link SplibBotGuard} loads this class only when the CAPTCHA is used,
 *     so it works without nanocaptcha as long as the CAPTCHA is not used.</p>
 */
final class CaptchaImageGenerator {

  private static final int CAPTCHA_WIDTH = 200;
  private static final int CAPTCHA_HEIGHT = 50;

  /** The created image and its answer. */
  record CaptchaImage(String answer, String dataUri) {
  }

  private CaptchaImageGenerator() {}

  /**
   * Creates a new image.
   *
   * @param length the number of characters
   * @return the answer and the image as a data URI ({@code data:image/png;base64,...})
   */
  static CaptchaImage create(int length) {
    ImageCaptcha captcha = new ImageCaptcha.Factory.Builder(CAPTCHA_WIDTH, CAPTCHA_HEIGHT)
        .addContent(new LatinContentProducer(length), new DefaultWordRenderer.Builder().build())
        .addBackground(new GradiatedBackgroundProducer()).addNoise(new CurvedLineNoiseProducer())
        .build().create();

    return new CaptchaImage(captcha.getContent(), captcha.toDataUri());
  }
}
