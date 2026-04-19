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
package jp.ecuacion.splib.jpa.advice;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import jp.ecuacion.lib.jpa.entity.EclibEntity;
import jp.ecuacion.splib.jpa.bean.SplibControllerAdviceInfoBean;
import jp.ecuacion.splib.jpa.repository.SplibRepository;
import jp.ecuacion.splib.jpa.util.SplibJpaFilterUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides the feature to delete the soft-deleted record physically
 * when inserting a new record with the same unique key as soft-deleted one, 
 * even though the soft-deleted record and the inserting record belnog to the other groups.
 * 
 * @see <a href="https://github.com/ecuacion-jp/ecuacion-splib/tree/main/ecuacion-splib-jpa">README</a> from github for details.
 */
public abstract class SplibSoftDeleteAdvice {

  @Autowired
  private SplibJpaFilterUtil filterUtil;
  @PersistenceContext
  private EntityManager em;

  /**
   * Provides the entrypoint of the feature.
   * 
   * @param joinPoint joinPoint
   */
  @SuppressWarnings("unchecked")
  @Before("execution(* *..*.base.repository.*.save(..))")
  public void onBeforeSave(JoinPoint joinPoint) {
    beforeAdvice((EclibEntity) joinPoint.getArgs()[0],
        (SplibRepository<EclibEntity, ?>) joinPoint.getThis());
  }

  /*
   * Deletes existing records to prevent duplicate errors when attempting to save (insert) a record
   * while a soft-deleted record with the same unique key exists.
   * This process must be executed with the soft-delete and group filter settings disabled
   * in order to handle the case where the same unique key value exists with soft-delete=true
   * in another group, and to process child table relationships when they exist.
   */
  private void beforeAdvice(EclibEntity entity, SplibRepository<EclibEntity, ?> repo) {

    // Not idiomatic Spring, but ThreadLocal is used because injection via @Bean etc. did not work.
    Object groupId = SplibControllerAdviceInfoBean.getGroupId();

    filterUtil.disableAllFilters();

    physicalDeleteSoftDeletedRecords(entity, repo);

    filterUtil.enableAllFilters(groupId);
  }

  private <T extends EclibEntity> void physicalDeleteSoftDeletedRecords(T entity,
      SplibRepository<T, ?> repo) {

    // boolean isUpdate = em.contains(entity);

    if (entity.hasSoftDeleteField()) {

      // if (isUpdate) {
      // Detach the entity once to disable dirty checking.
      // em.detach(entity);
      // }

      // Check for a soft-deleted record with the same ID and delete if found.
      // (Unlikely with the surrogate key strategy.)
      Optional<T> result = repo.findByIdAndSoftDeleteFieldTrueFromAllGroups(entity);
      if (result.isPresent()) {
        // Execute native query to avoid the change for persistence context.
        repo.deleteByIdAndSoftDeleteFieldTrueFromAllGroups(result.get());
        // repo.flush();
      }

      // Check for a soft-deleted record with the same unique key and delete if found.
      if (entity.hasNaturalKey()) {
        result = repo.findByNaturalKeyAndSoftDeleteFieldTrueFromAllGroups(entity);
        if (result.isPresent()) {
          // Execute native query to avoid the change for persistence context.
          repo.deleteByIdAndSoftDeleteFieldTrueFromAllGroups(result.get());
          // repo.flush();
        }
      }

      // if (isUpdate) {
      // Reattach the entity once detached.
      // entity = em.merge(entity);
      // repo.flush();
      // }
    }
  }
}
