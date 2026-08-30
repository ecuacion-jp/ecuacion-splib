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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;

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

  public String issueNewToken(HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    Set<String> tokenSet =
        (Set<String>) request.getSession().getAttribute(SESSION_KEY_TRANSACTION_TOKEN);

    if (tokenSet == null) {
      // LinkedHashSet to preserve insertion order, so the oldest token can be evicted first.
      tokenSet = new LinkedHashSet<String>();
      request.getSession().setAttribute(SESSION_KEY_TRANSACTION_TOKEN, tokenSet);
    }

    String newToken = RandomStringUtils.secure().nextAlphanumeric(40);
    tokenSet.add(newToken);

    while (tokenSet.size() > MAX_TOKENS_PER_SESSION) {
      Iterator<String> iterator = tokenSet.iterator();
      iterator.next();
      iterator.remove();
    }

    return newToken;
  }
}
