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
 * Is a string util called from thymeleaf.
 */
@Component("strUtil")
public class SplibThymeleafStringUtil {

  /** 
   * Change string like acc -> getAccList, acc.name -> getAcc_NameList.
   * 
   * <p>"_" is derived from repository query method definition  when relation is used.</p>
   */
  public String selectListMethodName(String itemName) {
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

  /** 
   * Change string like acc -> getAccList, acc.name -> getAcc().getNameList.
   */
  public String selectListMethodNameForEnum(String itemPropertyPath) {

    return getMethodName(itemPropertyPath) + "List";
  }

  /**
   * Change string like acc -> getAcc, acc.name -> getAcc().getName.
   */
  public String getMethodName(String itemPropertyPath) {
    String[] strs = itemPropertyPath.split("\\.");

    boolean is1st = true;
    StringBuilder rtn = new StringBuilder();
    for (int i = 0; i < strs.length; i++) {
      String str = strs[i];

      if (is1st) {
        is1st = false;

      } else {

        rtn.append("().get");
      }

      rtn.append(StringUtils.capitalize(str));
    }

    return "get" + rtn.toString();
  }
}
