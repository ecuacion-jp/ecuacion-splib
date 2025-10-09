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
package jp.ecuacion.splib.web.form;

import java.util.ArrayList;
import java.util.List;
import jp.ecuacion.splib.core.record.SplibRecord;

/**
 * Stores data for list.
 */
public class SplibListForm<T extends SplibRecord> extends SplibGeneralForm {
  private List<T> recList = new ArrayList<>();

  public List<T> getRecList() {
    return recList;
  }

  /**
   * Sets record list and returns this for method chain.
   * 
   * @param recList recList
   * @return {@code SplibListForm<T>}
   */
  public SplibListForm<T> setRecList(List<T> recList) {
    this.recList = recList;
    return this;
  }

}
