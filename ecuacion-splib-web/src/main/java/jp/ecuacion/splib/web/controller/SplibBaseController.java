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
   * Provides the feature that the item values in a request parameter changes all at once.
   * 
   * @param binder binder
   */
  @InitBinder
  public void initBinder(WebDataBinder binder) {
    // Change "" to null. This prevents java number properties (but String datatype in record class)
    // receive "" from html. (item values always string in html)
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

    // Change "" / null to boolean value when html switch value is "" / null
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
      // return false when submitted value is "" / null
      if (text == null || text.equals("") || text.equals("null")) {
        setValue(false);

      } else {
        // setValue(Boolean.valueOf(text));
        super.setAsText(text);
      }
    }
  }
}
