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
package jp.ecuacion.splib.web.bean;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import jp.ecuacion.splib.web.util.internal.TransactionTokenUtil;

/**
 * controllerの@RequestMappingなどがつけられた、外部からのurl実行により呼び出されるmethod時にredirectする場合の情報を保持するbean。
 */
public abstract class RedirectUrlBean {

  boolean isForward = false;

  /**
   * html request parameterは同一keyを許すのでvalueは配列としておく。
   * ただし、配列として取り扱うべき場面は多くないので、極力配列でないStringでも取り扱えるようにする。
   */
  private final Map<String, String[]> paramMap;

  public RedirectUrlBean() {
    this.paramMap = new HashMap<>();
  }

  protected String getParamsString() {
    // requestから来たparameterをまとめてRedirectUrlBeanに追加する場合、
    // transactionTokenが入ってきてしまうがそのままredirectするとチェックエラーになるのでtransactionTokenをparamsから除く処理を追加
    removeParam(TransactionTokenUtil.SESSION_KEY_TRANSACTION_TOKEN);
    
    // forwardの場合はそれとわかるparamterを追加
    if (isForward) {
      putParam("forward", "true");
    }

    boolean is1st = true;
    StringBuilder sb = new StringBuilder();
    // forwardのparamを追加
    for (Entry<String, String[]> entry : paramMap.entrySet()) {
      for (String value : entry.getValue()) {
        if (is1st) {
          is1st = false;
          sb.append("?");

        } else {
          sb.append("&");
        }

        sb.append(entry.getKey() + "=" + value);
      }
    }

    return sb.toString();
  }

  public RedirectUrlBean forward() {
    isForward = true;
    return this;
  }

  public boolean isForward() {
    return isForward;
  }

  protected String getProtocol() {
    return isForward ? "forward" : "redirect";
  }

  /**
   * paramMapへの追加。使いやすくmethod chainの形にしておく。
   */
  public RedirectUrlBean putParam(String key, String value) {
    Objects.requireNonNull(key);
    paramMap.put(key, value == null ? new String[] {""} : new String[] {value});

    return this;
  }

  /**
   * paramMapへの追加。使いやすくmethod chainの形にしておく。
   */
  public RedirectUrlBean putParam(String key, String[] values) {
    Objects.requireNonNull(key);
    paramMap.put(key, values == null ? new String[] {""} : values);

    return this;
  }

  /**
   * paramMapへの追加。使いやすくmethod chainの形にしておく。
   */
  public RedirectUrlBean putParams(String[][] paramPairs) {
    Objects.requireNonNull(paramPairs);

    for (int i = 0; i < paramPairs.length; i++) {
      Objects.requireNonNull(paramPairs[i]);

      String[] paramPair = paramPairs[i];
      Objects.requireNonNull(paramPair);
      if (paramPair.length < 2) {
        throw new RuntimeException("paramPairs[i].length must be equal to or greater than 2.");
      }

      List<String> list = Arrays.asList(paramPair);
      list.remove(0);
      String[] values = list.toArray(new String[list.size()]);

      Objects.requireNonNull(paramPair[0]);
      paramMap.put(paramPair[0], values);
    }

    return this;
  }

  /**
   * paramMapへの追加。使いやすくmethod chainの形にしておく。
   */
  public RedirectUrlBean putParamMap(Map<String, String[]> paramMap) {
    Objects.requireNonNull(paramMap);
    this.paramMap.putAll(paramMap);

    return this;
  }

  public RedirectUrlBean removeParam(String key) {
    paramMap.remove(key);
    return this;
  }
}
