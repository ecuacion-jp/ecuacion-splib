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
import java.util.Locale;
import jp.ecuacion.lib.core.util.VersionUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.ansi.Ansi8BitColor;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
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
 * <p>The {@code jp.ecuacion.splib.cli.banner-mode} property (e.g. in
 *     {@code application.properties}, or {@code --jp.ecuacion.splib.cli.banner-mode=white} on the
 *     command line — command-line arguments always win over {@code application.properties}, per
 *     Spring Boot's own property source precedence) selects one of {@link BannerMode}. This lets
 *     an app fix a mode for itself in {@code application.properties} while still letting an
 *     individual user override it at run time, e.g. when the app's chosen colors don't read well
 *     against that user's own terminal color scheme.</p>
 */
public class SplibCliBanner implements Banner {

  /** Property selecting the banner mode; see the class javadoc and {@link BannerMode}. */
  public static final String BANNER_MODE_PROPERTY = "jp.ecuacion.splib.cli.banner-mode";

  /**
   * The banner modes {@link #BANNER_MODE_PROPERTY} accepts.
   */
  public enum BannerMode {
    /** Prints nothing. */
    OFF,
    /** The default: each block in its own color, as designed. */
    COLOR,
    /** Every character printed in plain white, e.g. for a terminal with a dark background where
     *  the default colors don't read well. */
    WHITE,
    /** Every character printed in plain black, e.g. for a terminal with a light background where
     *  the default colors don't read well. */
    BLACK
  }

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
    BannerMode mode = resolveMode(environment);
    if (mode == BannerMode.OFF) {
      return;
    }

    final AnsiElement ecuacionColor = mode == BannerMode.COLOR ? ECUACION_COLOR : flatColor(mode);
    final AnsiElement appColor = mode == BannerMode.COLOR ? APP_COLOR : flatColor(mode);
    final AnsiElement versionColor = mode == BannerMode.COLOR ? VERSION_COLOR : flatColor(mode);

    out.println();
    printBlock(
        out, versionColor, AnsiOutput.toString(ecuacionColor, AnsiStyle.BOLD, ECUACION_MARK,
            AnsiStyle.NORMAL, ecuacionColor, TAGLINE_INDENT + TAGLINE),
        VersionUtil.getVersion("ecuacion-splib"));

    String springBootLabel = "(spring boot v" + SpringBootVersion.getVersion() + ")";
    int springBootPadding = Math.max(0, ECUACION_LINE_PLAIN.length() - springBootLabel.length());
    out.println(" ".repeat(springBootPadding) + AnsiOutput.toString(versionColor, springBootLabel));

    if (appName != null) {
      out.println();
      printBlock(out, versionColor, AnsiOutput.toString(appColor, APP_LABEL_INDENT + APP_LABEL,
          AnsiStyle.BOLD, appName, AnsiStyle.NORMAL), VersionUtil.getVersion(""));
    }

    out.println(AnsiOutput.toString(versionColor, "-".repeat(5)));
    out.println();
  }

  /**
   * Resolves {@link #BANNER_MODE_PROPERTY} to a {@link BannerMode}, defaulting to
   * {@link BannerMode#COLOR} when unset.
   *
   * @throws RuntimeException if the property is set to a value other than one of the
   *     {@link BannerMode} names (case-insensitive)
   */
  private BannerMode resolveMode(Environment environment) {
    String raw = environment.getProperty(BANNER_MODE_PROPERTY, BannerMode.COLOR.name());
    try {
      return BannerMode.valueOf(raw.toUpperCase(Locale.ROOT));

    } catch (IllegalArgumentException ex) {
      throw new RuntimeException("Invalid value for '" + BANNER_MODE_PROPERTY + "': '" + raw
          + "'. Valid values are: off, color, white, black.");
    }
  }

  /**
   * Returns the single flat foreground color {@code mode} (either {@link BannerMode#WHITE} or
   * {@link BannerMode#BLACK}) prints every character in.
   */
  private AnsiElement flatColor(BannerMode mode) {
    return mode == BannerMode.WHITE ? AnsiColor.WHITE : AnsiColor.BLACK;
  }

  /**
   * Prints one banner block: the given (already ANSI-encoded) label line, followed by a
   * right-aligned {@code "version: ..."} line (skipped when {@code version} is {@code null}).
   *
   * <p>The version line is always right-aligned against {@link #ECUACION_LINE_PLAIN}'s width
   *     (not each block's own label width), so both blocks' version columns line up.</p>
   *
   * @param versionColor the color to print the version line in
   * @param lineEncoded the label line to print, ANSI-encoded
   * @param version the version to show, or {@code null} to skip the version line
   */
  private void printBlock(PrintStream out, AnsiElement versionColor, String lineEncoded,
      @Nullable String version) {
    out.println(lineEncoded);

    if (version != null) {
      String versionLabel = "v" + version;
      int padding = Math.max(0, ECUACION_LINE_PLAIN.length() - versionLabel.length());
      out.println(" ".repeat(padding) + AnsiOutput.toString(versionColor, versionLabel));
    }
  }
}
