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
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;

public class TransactionTokenUtil {
  public static final String SESSION_KEY_TRANSACTION_TOKEN = "transactionToken";

  public String issueNewToken(HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    Set<String> tokenSet =
        (Set<String>) request.getSession().getAttribute(SESSION_KEY_TRANSACTION_TOKEN);

    if (tokenSet == null) {
      tokenSet = new HashSet<String>();
      request.getSession().setAttribute(SESSION_KEY_TRANSACTION_TOKEN, tokenSet);
    }

    String newToken = RandomStringUtils.randomAlphanumeric(40);
    tokenSet.add(newToken);

    return newToken;
  }
}
