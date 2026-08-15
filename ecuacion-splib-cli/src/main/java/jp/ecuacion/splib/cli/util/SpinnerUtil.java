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
package jp.ecuacion.splib.cli.util;

import java.io.Console;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Shows an animated "running..." indicator on the last console line while
 * {@code SplibCliRunner#execute} is in progress — the animation is a slowly growing/shrinking
 * run of dots appended directly after the given message (e.g. {@code "Running"} becomes
 * {@code "Running."}, {@code "Running.."}, {@code "Running..."}, {@code "Running"}, ...), so the
 * message itself should not already end with its own {@code "..."}.
 *
 * <p>While running, {@link System#out} and {@link System#err} are temporarily wrapped so that
 *     any output the app itself prints appears above the spinner line rather than mixed into
 *     it — the spinner line is erased just before such output is written, and reappears (via
 *     its own redraw loop) below it a moment later, keeping it pinned at the bottom.</p>
 *
 * <p>Disabled entirely when there is no real interactive terminal attached (output redirected to
 *     a file, etc.), since carriage-return/ANSI redraw sequences would otherwise litter that
 *     output instead of animating. Detected via {@link System#console()} being {@code null} —
 *     except on JDK 22+, where {@code System.console()} always returns non-null and
 *     {@code Console#isTerminal()} must be used instead; since this module compiles against
 *     JDK 21 (which has neither the method nor the ability to reference it directly), that check
 *     is done reflectively in {@link #isInteractiveTerminal()} so it still works correctly when
 *     the built jar is run on a newer JDK.</p>
 *
 * <p>{@code SplibCliApplication} calls {@link #start(String)} once and {@link #stop()} once per
 *     process, around its single {@code SplibCliRunner#execute} call, so static state is fine
 *     here — there is never more than one spinner in flight.</p>
 */
public final class SpinnerUtil {

  // Each frame is padded to the same width (that of "...") so the cursor stays in a fixed
  // column instead of drifting left/right as the dots grow and shrink.
  private static final String[] FRAMES = {".  ", ".. ", "...", "   "};

  private static final String CLEAR_LINE = "\r\033[K";

  private static final long FRAME_INTERVAL_MILLIS = 500L;

  private static final Object LOCK = new Object();

  private static String message = "";

  private static volatile boolean enabled;

  @Nullable
  private static PrintStream originalOut;

  @Nullable
  private static PrintStream originalErr;

  @Nullable
  private static Thread thread;

  private static volatile boolean running;

  private SpinnerUtil() {}

  /**
   * Returns whether a real interactive terminal is attached — see the class javadoc for why
   * this can't simply be {@code System.console() != null} on JDK 22+.
   */
  // The null check below is exactly what SystemConsoleNull warns against on its own, but it's
  // still correct pre-JDK-22 (where isTerminal() doesn't exist, checked reflectively above) and
  // is only the first of two checks on JDK 22+ (isTerminal() is what decides it there).
  @SuppressWarnings("SystemConsoleNull")
  private static boolean isInteractiveTerminal() {
    Console console = System.console();
    if (console == null) {
      return false;
    }

    try {
      Method isTerminal = Console.class.getMethod("isTerminal");
      return (boolean) Objects.requireNonNull(isTerminal.invoke(console));

    } catch (ReflectiveOperationException ex) {
      // Console#isTerminal() doesn't exist before JDK 22, but on those JDKs a non-null
      // console already means an interactive terminal is attached.
      return true;
    }
  }

  /**
   * Starts the spinner. Does nothing if disabled (see the class javadoc).
   *
   * @param message the message the animated dots are appended to, e.g. {@code "Running"} — see
   *     the class javadoc
   */
  @SuppressWarnings("null")
  public static void start(String message) {
    SpinnerUtil.message = message;
    enabled = isInteractiveTerminal();
    if (!enabled) {
      return;
    }

    originalOut = System.out;
    originalErr = System.err;
    System.setOut(wrap(originalOut));
    System.setErr(wrap(originalErr));

    running = true;
    thread = new Thread(SpinnerUtil::loop, "splib-cli-spinner");
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Stops the spinner, restores {@link System#out}/{@link System#err}, and clears the spinner
   * line. Does nothing if disabled.
   */
  @SuppressWarnings("null")
  public static void stop() {
    if (!enabled) {
      return;
    }

    running = false;
    if (thread != null) {
      thread.interrupt();
      try {
        thread.join(FRAME_INTERVAL_MILLIS * 2);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }

    if (originalOut != null) {
      System.setOut(originalOut);
    }
    if (originalErr != null) {
      System.setErr(originalErr);
    }

    synchronized (LOCK) {
      if (originalOut != null) {
        originalOut.print(CLEAR_LINE);
        originalOut.flush();
      }
    }
  }

  private static void loop() {
    int frameIndex = 0;
    while (running) {
      draw(FRAMES[frameIndex % FRAMES.length]);
      frameIndex++;
      try {
        Thread.sleep(FRAME_INTERVAL_MILLIS);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  @SuppressWarnings("null")
  private static void draw(String frame) {
    synchronized (LOCK) {
      if (originalOut == null) {
        return;
      }
      originalOut.print(CLEAR_LINE + message + frame + " ");
      originalOut.flush();
    }
  }

  /**
   * Wraps {@code target} so that every write first erases the current spinner line, letting the
   * real output take its place; the spinner reappears on its own on the next redraw.
   */
  private static PrintStream wrap(PrintStream target) {
    return new PrintStream(new OutputStream() {

      @Override
      public void write(int b) {
        synchronized (LOCK) {
          target.print(CLEAR_LINE);
          target.write(b);
          target.flush();
        }
      }

      @Override
      public void write(byte @Nullable [] b, int off, int len) {
        synchronized (LOCK) {
          target.print(CLEAR_LINE);
          target.write(b, off, len);
          target.flush();
        }
      }
    }, true);
  }
}
