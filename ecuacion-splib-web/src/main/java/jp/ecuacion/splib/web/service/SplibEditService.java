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

import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.splib.web.form.SplibEditForm;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Provides abstract edit service.
 * 
 * @param <F> SplibEditForm
 */
public abstract class SplibEditService<F extends SplibEditForm>
    extends SplibGeneral1FormService<F> {

  @Override
  public void page(F form, UserDetails loginUser) throws Exception {
    
    if (form.isInsert() == null) {
      throw new RuntimeException("splibEditForm.isInsert() must not be null.");

    } else if (form.isInsert()) {
      getInsertPage(form, loginUser);

    } else {
      getUpdatePage(form, loginUser);
    }
  }

  /**
   * Shows insert page.
   * 
   * <p>The argument form is used 
   *     to receive record id and version from the searchList Controller.</p>
   * 
   * @param form form
   * @param loginUser loginUser
   * @throws AppException AppException
   */
  public abstract void getInsertPage(F form, UserDetails loginUser)
      throws AppException;

  /**
   * Shows update page.
   * 
   * <p>The argument form is used 
   *     to receive record id and version from the searchList Controller.</p>
   * 
   * @param form form
   * @param loginUser loginUser
   * @throws AppException AppException
   */
  public abstract void getUpdatePage(F form, UserDetails loginUser)
      throws Exception;

  /**
   * Edits (=inserts or updates) the data.
   * 
   * @param form form
   * @param loginUser loginUser
   * @throws Exception Exception
   */
  public abstract void edit(F form, UserDetails loginUser) throws Exception;

  /**
   * Determines if the page is insert or update.
   * 
   * @param id id
   * @return {@code true} when insert.
   */
  protected boolean isInsert(String id) {
    return id == null || id.equals("");
  }
}
