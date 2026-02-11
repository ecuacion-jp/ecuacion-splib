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

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import jp.ecuacion.lib.core.item.EclibItemContainer;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import org.springframework.stereotype.Component;

/**
 * Offers utility methods for components.
 */
@Component("compUtil")
public class SplibThymeleafComponentUtil {

  private HttpServletRequest request;

  /**
   * Constructs a new instance.
   * 
   * @param request request
   */
  public SplibThymeleafComponentUtil(HttpServletRequest request) {
    this.request = request;
  }

  /**
   * Provides message string shown when the delete button pressed.
   * 
   * <p>itemDisplayedOnDelete is able to have multiple items like "itemA,itemB".
   *     In that case the resultant string is like '(itemA, itemB) : (1, 2)'.</p>
   */
  public String getDeleteConfirmMessage(EclibItemContainer rootRecord,
      String itemDisplayedOnDelete) {
    List<String> fieldNameList = Arrays.asList(itemDisplayedOnDelete.split(","));

    boolean multiple = fieldNameList.size() > 1;
    StringBuilder fieldNameSb = new StringBuilder();

    for (String itemPropertyPath : fieldNameList) {
      String itemName = PropertyFileUtil.getItemName(request.getLocale(),
          rootRecord.getItem(itemPropertyPath).getItemNameKey());
      fieldNameSb.append(", " + itemName);
    }

    String fieldNames = (multiple ? " (" : "")
        + (fieldNameList.size() == 0 ? "" : fieldNameSb.toString().substring(2))
        + (multiple ? ") " : "");

    return PropertyFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.deleteConfirmation",
        new String[] {fieldNames.toString()});
  }
}
