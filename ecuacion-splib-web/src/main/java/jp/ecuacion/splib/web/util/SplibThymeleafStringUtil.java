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
package jp.ecuacion.splib.web.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * thymeleaf側から呼ばれる想定のクラス。thymeleaf上で必要となる文字列処理を実施.
 */
@Component("strUtil")
public class SplibThymeleafStringUtil {
  
  /** 
   * acc -> getAccList, acc.name -> getAcc_NameListに変更.
   * 
   * <p>"_"はrelation使用時のrepositoryの使用方法に準じた。</p>
   */
  public String selectListMethodName(String itemName) {
    // "."で区切ったリストを作り、そのそれぞれの先頭をcapitalizeし、その後でリストの文字列を"_"を間に入れて結合。
    String[] strs = itemName.split("\\.");
    
    boolean is1st = true;
    StringBuilder rtn = new StringBuilder();
    for (String str : strs) {
      if (is1st) {
        is1st = false;
      } else {
        rtn.append("_");
      }
      
      rtn.append(StringUtils.capitalize(str));
    }
    
    return "get" + rtn.toString() + "List";
  }
}
