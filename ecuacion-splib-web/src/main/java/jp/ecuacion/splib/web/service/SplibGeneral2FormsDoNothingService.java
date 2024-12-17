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

import jp.ecuacion.splib.web.form.SplibGeneralForm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

//@formatter:off
@Service
public class SplibGeneral2FormsDoNothingService
    <F1 extends SplibGeneralForm, F2 extends SplibGeneralForm>
    extends SplibGeneral2FormsService<F1, F2> {
  //@formatter:on


  @Override
  public void page(F1 form1, F2 form2, UserDetails loginUser) throws Exception {

  }

  @Override
  public void prepareForm(F1 form1, F2 form2, UserDetails loginUser) {

  }
}
