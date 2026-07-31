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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Authenticates a presented credential against a single {@code password-xxx} property out of a
 * family sharing a common prefix (e.g. {@code jp.ecuacion.splib.web.builtin-admin-login},
 * {@code jp.ecuacion.splib.rest.builtin-api-key}), so that {@code ecuacion-splib}'s various
 * built-in, property-configured credentials (web's built-in admin login, REST's built-in API
 * key, ...) share one convention and one comparison implementation instead of each module
 * reinventing it.
 *
 * <p>Exactly one of {@code prefix + ".password-plain"} or {@code prefix + ".password-bcrypt"} is
 *     expected to be set. Callers treat the possible outcomes as follows:</p>
 * <ul>
 * <li>{@link Outcome#NOT_CONFIGURED} — neither is set. Fail closed (reject every credential);
 *     this is indistinguishable from a wrong credential to whoever is presenting one, by
 *     design.</li>
 * <li>{@link Outcome#MATCHED} / {@link Outcome#NOT_MATCHED} — exactly one is set, and the
 *     presented value did (or didn't) match it.</li>
 * <li>{@link Outcome#MISCONFIGURED} — both are set. Unlike {@code NOT_CONFIGURED}, this is safe
 *     (and desirable) to surface distinctly, since it can only be reached by whoever controls
 *     {@code application.properties}, never by an external caller presenting credentials.</li>
 * </ul>
 */
public class SplibHashedPropertyResolver {

  private static final String SUFFIX_PLAIN = "password-plain";
  private static final String SUFFIX_BCRYPT = "password-bcrypt";

  private SplibHashedPropertyResolver() {}

  /** The possible outcomes of {@link #authenticate(String, String)}. */
  public enum Outcome {
    MATCHED, NOT_MATCHED, NOT_CONFIGURED, MISCONFIGURED
  }

  /** Holds the outcome of {@link #authenticate(String, String)}. */
  public static final class Result {

    private final Outcome outcome;
    private final List<String> configuredKeys;

    private Result(Outcome outcome, List<String> configuredKeys) {
      this.outcome = outcome;
      this.configuredKeys = configuredKeys;
    }

    public Outcome getOutcome() {
      return outcome;
    }

    /**
     * The full property keys (prefix included) that were found set — empty if neither was, one
     * if {@code MATCHED}/{@code NOT_MATCHED}, both if {@code MISCONFIGURED}. Intended for
     * building a misconfiguration error message naming the offending keys.
     */
    public List<String> getConfiguredKeys() {
      return configuredKeys;
    }
  }

  /**
   * Reads {@code propertyPrefix + ".password-plain"} and {@code propertyPrefix +
   * ".password-bcrypt"}, and compares {@code presentedValue} against whichever single one (if
   * any) is set.
   *
   * @param propertyPrefix the shared prefix, without a trailing dot, e.g.
   *     {@code "jp.ecuacion.splib.web.builtin-admin-login"}
   * @param presentedValue the value presented by whoever is being authenticated
   * @return the outcome
   */
  public static Result authenticate(String propertyPrefix, String presentedValue) {
    String plainKey = propertyPrefix + "." + SUFFIX_PLAIN;
    String bcryptKey = propertyPrefix + "." + SUFFIX_BCRYPT;

    String plainValue = PropertiesFileUtil.getApplicationOrElse(plainKey, null);
    String bcryptValue = PropertiesFileUtil.getApplicationOrElse(bcryptKey, null);

    List<String> configuredKeys = new ArrayList<>();
    if (plainValue != null && !plainValue.isEmpty()) {
      configuredKeys.add(plainKey);
    }
    if (bcryptValue != null && !bcryptValue.isEmpty()) {
      configuredKeys.add(bcryptKey);
    }

    if (configuredKeys.size() > 1) {
      return new Result(Outcome.MISCONFIGURED, configuredKeys);
    }

    if (configuredKeys.isEmpty()) {
      return new Result(Outcome.NOT_CONFIGURED, configuredKeys);
    }

    boolean matched = plainValue != null
        ? MessageDigest.isEqual(presentedValue.getBytes(StandardCharsets.UTF_8),
            plainValue.getBytes(StandardCharsets.UTF_8))
        : new BCryptPasswordEncoder().matches(presentedValue, bcryptValue);

    return new Result(matched ? Outcome.MATCHED : Outcome.NOT_MATCHED, configuredKeys);
  }
}
