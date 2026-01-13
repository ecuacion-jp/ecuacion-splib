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
   * soft deleteされた状態のレコードがある状態でsave(insert)しようとしたときに、重複エラーが発生しないため
   * 既存レコードを削除する。
   * unique keyの同一値が削除フラグtrueで別groupに存在する場合でも対応かであること、
   * relationにより子テーブルとの関連が存在する場合にはそれらも含めて処理できること、を満たすため、
   * これらの処理は、削除フラグ・グループのfilter設定をoffにした状態で実施する前提とする。
   */
  private void beforeAdvice(EclibEntity entity, SplibRepository<EclibEntity, ?> repo) {

    // springっぽくないのだが、@BeanなどでうまくできなかったのでThreadLocalを使用して値を取得
    Object groupId = SplibControllerAdviceInfoBean.getGroupId();

    filterUtil.disableAllFilters();

    physicalDeleteSoftDeletedRecords(entity, repo);

    filterUtil.enableAllFilters(groupId);
  }

  private <T extends EclibEntity> void physicalDeleteSoftDeletedRecords(T entity,
      SplibRepository<T, ?> repo) {

    boolean isUpdate = em.contains(entity);

    if (entity.hasSoftDeleteField()) {

      if (isUpdate) {
        // Detach the entity once to disable dirty checking.
        em.flush();
        em.detach(entity);
      }

      // 同一IDで削除済みのレコード存在チェック。あれば削除。（surrogate key strategyでは発生する場面は考えにくいが）
      Optional<T> result = repo.findByIdAndSoftDeleteFieldTrueFromAllGroups(entity);
      if (result.isPresent()) {
        repo.delete(result.get());
        repo.flush();
      }

      // 同一Unique Keyで削除済みのレコード存在チェック。あれば削除
      if (entity.hasNaturalKey()) {
        result = repo.findByNaturalKeyAndSoftDeleteFieldTrueFromAllGroups(entity);
        if (result.isPresent()) {
          repo.delete(result.get());
          repo.flush();
        }
      }

      if (isUpdate) {
        // Reattach the entity once detached.
        entity = em.merge(entity);
      }
    }
  }
}
