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
package jp.ecuacion.splib.web.service;

import java.util.List;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import org.springframework.security.core.userdetails.UserDetails;

public abstract class SplibGeneral1FormService<F extends SplibGeneralForm>
    extends SplibGeneralService {

  @Override
  @SuppressWarnings("unchecked")
  public void prepareForm(List<SplibGeneralForm> allFormList, UserDetails loginUser) {
    if (allFormList == null || allFormList.size() != 1) {
      throw new RuntimeException("The number of forms not match.");
    }

    SplibGeneralForm form = allFormList.get(0);
    prepareForm((F) form, loginUser);
  }

  public abstract void prepareForm(F form, UserDetails loginUser);

  public abstract void page(F form, UserDetails loginUser) throws Exception;
}
