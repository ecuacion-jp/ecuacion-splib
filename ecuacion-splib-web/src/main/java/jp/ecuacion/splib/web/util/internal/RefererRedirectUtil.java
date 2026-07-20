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
package jp.ecuacion.splib.web.util.internal;

import java.net.URI;

/**
 * Extracts a safe, same-origin redirect target from a client-supplied {@code Referer} header.
 *
 * <p>Used by exception handlers that redirect the user back to "the page they came from" after
 *     an error, taking the target from the {@code Referer} header since the app has no other
 *     record of it at that point.</p>
 */
public class RefererRedirectUtil {

  private RefererRedirectUtil() {}

  /**
   * Returns the path (plus query, if any) of {@code referer}, for use as a same-origin
   * {@code "redirect:"} target — never the scheme or host, since {@code referer} is entirely
   * client-controlled and must never be able to send the user off-site (open redirect /
   * phishing, CWE-601).
   *
   * <p><strong>Why a same-slash check is required in addition to dropping the scheme/host:</strong>
   *     {@link URI#getRawPath()} is expected to strip the scheme and authority (host) from a
   *     {@code Referer} like {@code https://evil.example/x}, leaving only {@code /x}. That much
   *     holds for a "clean" {@code //host/path} input too: {@code URI.create("//evil.example/x")}
   *     parses {@code evil.example} as the authority, again leaving {@code getRawPath()} as just
   *     {@code /x}. But when the input already carries its own scheme and authority and the
   *     *path component itself* starts with an extra {@code "//"} — e.g.
   *     {@code Referer: https://attacker.example//evil.example/x}, a URL an attacker can host
   *     as-is and link a victim through, so the browser sends it verbatim as {@code Referer} —
   *     {@code URI} parses {@code attacker.example} as the authority and returns
   *     {@code //evil.example/x} unchanged as {@code getRawPath()}, because a path is allowed to
   *     contain slashes and {@code URI} does not re-interpret it. (The same happens with a
   *     scheme-less input that repeats the {@code //} marker four or more times, e.g.
   *     {@code ////evil.example/x}.)</p>
   *
   * <p>Concatenated after {@code "redirect:"}, that value reaches Spring's {@code RedirectView}
   *     unchanged (Spring Boot's default context path is empty, so nothing is prepended), and
   *     the resulting {@code Location: //evil.example/x} response header is interpreted by every
   *     browser as a <em>protocol-relative</em> absolute URL — i.e. same scheme, different host —
   *     sending the user to {@code evil.example} instead of back into this app.</p>
   *
   * <p>The fix is to require the extracted target to start with exactly one {@code "/"}: if it
   *     starts with {@code "//"} (or, degenerately, doesn't start with {@code "/"} at all), the
   *     input is discarded and {@code "/"} is returned instead of trusting it.</p>
   *
   * @param referer the raw {@code Referer} header value; must not be {@code null} (callers are
   *     expected to skip calling this method when the header is absent)
   * @return a same-origin path beginning with exactly one {@code "/"}, optionally followed by
   *     {@code "?"} and the raw query string; {@code "/"} if {@code referer} has no path or the
   *     extracted path fails the same-origin check above
   * @throws IllegalArgumentException if {@code referer} is not a valid URI; callers are expected
   *     to catch this themselves so they can log it in their own context
   */
  public static String toSameOriginRedirectTarget(String referer) {
    URI uri = URI.create(referer);
    String path = uri.getRawPath();
    String target = (path == null || path.isEmpty()) ? "/" : path;

    if (!target.startsWith("/") || target.startsWith("//")) {
      return "/";
    }

    if (uri.getRawQuery() != null) {
      target += "?" + uri.getRawQuery();
    }

    return target;
  }
}
