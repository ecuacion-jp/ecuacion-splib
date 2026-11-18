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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.util.WebUtils;

/**
 * Issues and consumes the transaction tokens held in the http session.
 *
 * <p>The token set is shared by every request of the same session, and those requests can run
 *     concurrently (multiple tabs, browser prefetch, crawlers keeping cookies, etc.).
 *     Since {@code LinkedHashSet} is not thread-safe, unsynchronized concurrent access corrupts it
 *     (e.g. its size no longer matches its elements, making {@code iterator().next()} throw
 *     {@code NoSuchElementException} on every later request of that session).
 *     So every access to the token set is done in this class
 *     while holding the session mutex ({@link WebUtils#getSessionMutex(HttpSession)}).</p>
 */
public class TransactionTokenUtil {
  public static final String SESSION_KEY_TRANSACTION_TOKEN = "transactionToken";

  /**
   * Upper bound on the number of tokens held per session at once.
   *
   * <p>A token is normally removed as soon as its page is submitted, so the set holds only the
   *     tokens of pages currently open awaiting submission. This allows headroom for a handful of
   *     concurrently open tabs/pages on the same session, while still preventing a session that
   *     keeps issuing tokens without ever submitting (e.g. repeated GETs of a {@code permitAll}
   *     page) from accumulating them without bound.</p>
   */
  private static final int MAX_TOKENS_PER_SESSION = 10;

  /**
   * Issues a new token and stores it in the session.
   *
   * @param request request
   * @return the new token
   */
  public String issueNewToken(HttpServletRequest request) {
    HttpSession session = request.getSession();
    String newToken = RandomStringUtils.secure().nextAlphanumeric(40);

    synchronized (WebUtils.getSessionMutex(session)) {
      @SuppressWarnings("unchecked")
      Set<String> tokenSet = (Set<String>) session.getAttribute(SESSION_KEY_TRANSACTION_TOKEN);

      if (tokenSet == null) {
        // LinkedHashSet to preserve insertion order, so the oldest token can be evicted first.
        tokenSet = new LinkedHashSet<String>();
        session.setAttribute(SESSION_KEY_TRANSACTION_TOKEN, tokenSet);
      }

      tokenSet.add(newToken);

      while (tokenSet.size() > MAX_TOKENS_PER_SESSION) {
        Iterator<String> iterator = tokenSet.iterator();
        iterator.next();
        iterator.remove();
      }
    }

    return newToken;
  }

  /**
   * Consumes {@code token}: removes it from the session if it is held there.
   *
   * <p>The check and the removal are done atomically,
   *     so the same token cannot be accepted by two concurrent requests (double submission).</p>
   *
   * @param request request
   * @param token the token sent from the html page
   * @return {@code true} if the token was valid (held in the session) and has been consumed,
   *     or if the session holds no token set yet (= nothing to check against);
   *     {@code false} if the token is not held in the session
   */
  public boolean consumeToken(HttpServletRequest request, String token) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return true;
    }

    synchronized (WebUtils.getSessionMutex(session)) {
      @SuppressWarnings("unchecked")
      Set<String> tokenSet = (Set<String>) session.getAttribute(SESSION_KEY_TRANSACTION_TOKEN);

      return tokenSet == null || tokenSet.remove(token);
    }
  }
}
