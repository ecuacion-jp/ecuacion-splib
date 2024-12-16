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

import jakarta.persistence.EntityManager;
import jp.ecuacion.lib.jpa.entity.AbstractEntity;
import jp.ecuacion.splib.web.form.SplibEditForm;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
public abstract class SplibEditJpaService<F extends SplibEditForm, E extends AbstractEntity>
    extends SplibEditService<F> implements SplibJpaServiceInterface<E> {

  protected EntityManager em;

  public void setEntityManager(EntityManager em) {
    this.em = em;
  }
}
