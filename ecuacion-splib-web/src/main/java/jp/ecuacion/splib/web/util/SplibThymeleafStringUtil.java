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

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import jp.ecuacion.lib.core.util.PropertyPathUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * Is a string util called from thymeleaf.
 */
@Component("strUtil")
public class SplibThymeleafStringUtil {

  /** 
   * Change string like acc -> getAccList, acc.name -> getAcc_NameList.
   * 
   * <p>"_" is derived from repository query method definition  when relation is used.</p>
   *
   * <p>Indexes of lists are ignored: {@code rows[3].name} and {@code rows[].name}
   *     both result in {@code getRows_NameList}.</p>
   */
  public String selectListMethodName(String itemName) {
    String[] strs =
        PropertyPathUtil.toIndexlessPath(itemName).replace("[]", "").split("\\.", -1);

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
    String[] strs = itemPropertyPath.split("\\.", -1);

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

  /**
   * Returns whether a checkbox option is selected, comparing each selected value exactly.
   *
   * <p>A partial match is not enough: option {@code 1} must not be selected by value {@code 10}.
   *     A {@code String} is treated as comma-separated values since that is how Spring binds
   *     multiple checked values to a {@code String} field.</p>
   *
   * @param selectedValues selected values: a {@code Collection}, an array or a single value
   * @param optionValue the value of the option
   * @return {@code true} if {@code optionValue} is one of {@code selectedValues}
   */
  public boolean isSelected(@Nullable Object selectedValues, @Nullable Object optionValue) {
    if (selectedValues == null || optionValue == null) {
      return false;
    }

    Stream<?> values;
    if (selectedValues instanceof Collection<?> collection) {
      values = collection.stream();

    } else if (ObjectUtils.isArray(selectedValues)) {
      values = Arrays.stream(ObjectUtils.toObjectArray(selectedValues));

    } else {
      values = Arrays.stream(selectedValues.toString().split(",", -1)).map(String::trim);
    }

    String option = optionValue.toString();
    return values.anyMatch(value -> value != null && option.equals(value.toString()));
  }
}
