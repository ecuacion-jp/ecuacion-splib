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
package jp.ecuacion.splib.cli.banner;

import java.io.PrintStream;
import jp.ecuacion.lib.core.util.VersionUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.ansi.Ansi8BitColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.core.env.Environment;

/**
 * Prints ecuacion-splib-cli's default startup banner: a compact brand mark, no ASCII art.
 *
 * <p>Optionally shows a second block with the app's own name and version, when the app passes
 *     its name via {@code SplibCliApplication.main(cls, args, appName)}. The app's own version
 *     is read via {@code VersionUtil.getVersion("")}, the same mechanism
 *     {@code ecuacion-splib-web}'s config screen uses — the app just needs its own
 *     {@code version.properties} (see the ecuacion-splib-cli reference docs).</p>
 *
 * <p>{@code AnsiOutput} automatically falls back to plain text (no escape codes) when the
 *     terminal isn't detected as ANSI-capable, so this reads fine without color too.</p>
 *
 * <p>Printing can be turned off with the {@code jp.ecuacion.splib.cli.banner-mode} property
 *     (e.g. in {@code application.properties}, or {@code --jp.ecuacion.splib.cli.banner-mode=off}
 *     on the command line), set to {@code on} (default) or {@code off}. Unlike Spring Boot's own
 *     {@code spring.main.banner-mode} (which also has a {@code log} option, since Spring's banner
 *     can be routed to the log instead of the console), this banner is console-only, so there is
 *     no third mode to mirror.</p>
 */
public class SplibCliBanner implements Banner {

  /** Property to turn the banner on/off; see the class javadoc. */
  public static final String BANNER_MODE_PROPERTY = "jp.ecuacion.splib.cli.banner-mode";

  private static final Ansi8BitColor ECUACION_COLOR = Ansi8BitColor.foreground(25);

  private static final Ansi8BitColor APP_COLOR = Ansi8BitColor.foreground(168);

  private static final Ansi8BitColor VERSION_COLOR = Ansi8BitColor.foreground(244);

  private static final String ECUACION_MARK = "= ecuacion";

  private static final String TAGLINE_INDENT = "  ";

  private static final String TAGLINE = "command line interface";

  private static final String ECUACION_LINE_PLAIN = ECUACION_MARK + TAGLINE_INDENT + TAGLINE;

  private static final String APP_LABEL = "app  ";

  /** Left-padding for {@link #APP_LABEL} so its own following text starts at the same column
   *  {@link #TAGLINE} does on the ecuacion line. */
  private static final String APP_LABEL_INDENT =
      " ".repeat(Math.max(0, (ECUACION_MARK + TAGLINE_INDENT).length() - APP_LABEL.length()));

  @Nullable
  private final String appName;

  /**
   * Constructs a new instance that shows only the ecuacion-splib-cli block.
   */
  public SplibCliBanner() {
    this(null);
  }

  /**
   * Constructs a new instance.
   *
   * @param appName the app's display name, shown in a second block below the ecuacion-splib-cli
   *     block along with the app's own version (via {@code VersionUtil.getVersion("")}); when
   *     {@code null}, that second block is omitted entirely
   */
  public SplibCliBanner(@Nullable String appName) {
    this.appName = appName;
  }

  @Override
  public void printBanner(Environment environment, @Nullable Class<?> sourceClass,
      PrintStream out) {
    if ("off".equalsIgnoreCase(environment.getProperty(BANNER_MODE_PROPERTY, "on"))) {
      return;
    }

    out.println();
    printBlock(out,
        AnsiOutput.toString(ECUACION_COLOR, AnsiStyle.BOLD, ECUACION_MARK, AnsiStyle.NORMAL,
            ECUACION_COLOR, TAGLINE_INDENT + TAGLINE),
        VersionUtil.getVersion("ecuacion-splib"));

    String springBootLabel = "(spring boot v" + SpringBootVersion.getVersion() + ")";
    int springBootPadding = Math.max(0, ECUACION_LINE_PLAIN.length() - springBootLabel.length());
    out.println(" ".repeat(springBootPadding) + AnsiOutput.toString(VERSION_COLOR,
        springBootLabel));

    if (appName != null) {
      out.println();
      printBlock(out,
          AnsiOutput.toString(APP_COLOR, APP_LABEL_INDENT + APP_LABEL, AnsiStyle.BOLD, appName,
              AnsiStyle.NORMAL),
          VersionUtil.getVersion(""));
    }

    out.println(AnsiOutput.toString(VERSION_COLOR, "-".repeat(5)));
    out.println();
  }

  /**
   * Prints one banner block: the given (already ANSI-encoded) label line, followed by a
   * right-aligned {@code "version: ..."} line (skipped when {@code version} is {@code null}).
   *
   * <p>The version line is always right-aligned against {@link #ECUACION_LINE_PLAIN}'s width
   *     (not each block's own label width), so both blocks' version columns line up.</p>
   *
   * @param lineEncoded the label line to print, ANSI-encoded
   * @param version the version to show, or {@code null} to skip the version line
   */
  private void printBlock(PrintStream out, String lineEncoded, @Nullable String version) {
    out.println(lineEncoded);

    if (version != null) {
      String versionLabel = "v" + version;
      int padding = Math.max(0, ECUACION_LINE_PLAIN.length() - versionLabel.length());
      out.println(" ".repeat(padding) + AnsiOutput.toString(VERSION_COLOR, versionLabel));
    }
  }
}
