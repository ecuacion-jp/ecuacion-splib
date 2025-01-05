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
package jp.ecuacion.splib.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Provides abstract base controller.
 */
public abstract class SplibBaseController {

  @Autowired
  protected HttpServletRequest request;

  /**
   * Provides the feature that the items in a request parameter changes all at once.
   * 
   * @param binder binder
   */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    // 文字列項目の""をnullに変更。これにより、実際には数字だが””が来るなどを防げる。
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    // Boolean項目で、offのときにnullでなくfalseとなるよう対応（redmine#323参照）
    binder.registerCustomEditor(Boolean.class, new NullSupportCustomBooleanEditor());
  }

  /**
   * Provides the PropertyEditor which change null to false.
   */
  static class NullSupportCustomBooleanEditor extends CustomBooleanEditor {
    public NullSupportCustomBooleanEditor() {
      super(false);
    }

    @Override
    public void setAsText(@Nullable String text) throws IllegalArgumentException {
      // checkboxでは以下は発生しないはずなのだが一応対処
      if (text == null || text.equals("") || text.equals("null")) {
        setValue(null);
        return;
      }

      text = text.trim();

      // redmine #323対応により、複数の値がカンマ区切りで場合がある。その場合はtrueで返すのだが、
      // #323の前提はonとoffがくることなので、そうでない場合はエラーとする。
      List<String> list = Arrays.asList(text.split(","));
      if (list.size() > 1) {
        if (list.size() > 2) {
          throw new RuntimeException("Unpresumable.");
        }

        Collections.sort(list);
        if (!(list.get(0).equalsIgnoreCase("OFF") && list.get(1).equalsIgnoreCase("ON"))) {
          throw new RuntimeException("Unpresumable.");
        }

        setValue(Boolean.TRUE);
        return;
      }

      super.setAsText(text);
    }
  }
}
