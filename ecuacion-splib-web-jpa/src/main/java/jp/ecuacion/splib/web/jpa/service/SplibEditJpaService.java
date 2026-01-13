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
package jp.ecuacion.splib.web.jpa.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jp.ecuacion.lib.jpa.entity.EclibEntity;
import jp.ecuacion.splib.jpa.repository.SplibRepository;
import jp.ecuacion.splib.web.form.SplibEditForm;
import jp.ecuacion.splib.web.service.SplibEditService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides abstract edit service.
 * 
 * @param <F> SplibEditForm
 * @param <E> AbstractEntity
 */
@Transactional(rollbackFor = Exception.class)
public abstract class SplibEditJpaService<F extends SplibEditForm, E extends EclibEntity>
    extends SplibEditService<F> implements SplibJpaServiceInterface<E> {

  @PersistenceContext
  protected EntityManager em;

  /**
   * Offers a utility function to upsert database safely in the library.
   * 
   * <p>In jpa or spring data jpa world, what you have to do to insert is to 
   *     {@code repository.save(e)}.<br>
   *     And no need to do anything to update thanks to "dirty checking"
   *     (jpa automatically detects and updates changes of managed entities on commit).</p>
   * 
   * <p>But the feature of the library conflicts with the dirty checking feature.<br>
   *     The library supports "soft delete flag" feature, which should delete key-duplicated 
   *     soft-deleted record on insert or update.<br><br>
   *     We can hook "insert" and delete duplicated soft-deleted record
   *     because "save()" method is always called (it's done in {@code SplibSoftDeleteAdvice}), 
   *     but "update" we cannot because no method is called 
   *     during the update derived from "dirty checking".<br><br>
   *     To prevent primary key duplication (which does not happen in principle 
   *     because the library needs surrogate key strategy) or unique key dupliication on update
   *     in the library environment, we need to use {@code save()} to realize hook.<br>
   *     But it's not enough because when you call {@code save()}, 
   *     updates derived from "dirty checking" processed right before the hook procedure processed.
   *     That's why in this method "detach()" firstly to disable "dirty checking",
   *     then save to prevent key duplication on update.<br><br>
   *     Strictly speaking, {@code detach()} is not needed on insert, 
   *     but "insertOrUpdate" method is created for simplicity.
   * </p>
   */
  protected <T extends EclibEntity> T insertOrUpdate(SplibRepository<T, ?> repo, T e) {

    return repo.save(e);
  }
}
