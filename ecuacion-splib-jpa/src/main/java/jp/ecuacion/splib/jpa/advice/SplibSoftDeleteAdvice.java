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

import java.util.Optional;
import jp.ecuacion.lib.jpa.entity.AbstractEntity;
import jp.ecuacion.splib.jpa.bean.SplibSoftDeleteAdviceInfoBean;
import jp.ecuacion.splib.jpa.repository.SplibRepository;
import jp.ecuacion.splib.jpa.util.SplibJpaFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SplibSoftDeleteAdvice {

  @Autowired
  private SplibJpaFilterUtil filterUtil;
  
  /** 
   * soft deleteされた状態のレコードがある状態でsave(insert)しようとしたときに、重複エラーが発生しないため
   * 既存レコードを削除する。
   * unique keyの同一値が削除フラグtrueで別groupに存在する場合でも対応かであること、
   * relationにより子テーブルとの関連が存在する場合にはそれらも含めて処理できること、を満たすため、
   * これらの処理は、削除フラグ・グループのfilter設定をoffにした状態で実施する前提とする。
   */
  public void beforeAdvice(AbstractEntity entity, SplibRepository<AbstractEntity, ?> repo) {

    // springっぽくないのだが、@BeanなどでうまくできなかったのでThreadLocalを使用して値を取得
    Object groupId = SplibSoftDeleteAdviceInfoBean.getGroupId();

    disableAllFilters(groupId);

    physicalDeleteSoftDeletedRecords(entity, repo);

    enableAllFilters(groupId);
  }

  private void physicalDeleteSoftDeletedRecords(AbstractEntity entity,
      SplibRepository<AbstractEntity, ?> repo) {
    if (entity.hasSoftDeleteField()) {
      // 同一IDで削除済みのレコード存在チェック。あれば削除。（surrogate key strategyでは発生する場面は考えにくいが）
      Optional<AbstractEntity> result = repo.findByIdAndSoftDeleteFieldTrueFromAllGroups(entity);
      if (result.isPresent()) {
        
        repo.delete(result.get());
        // sessionのfilterを変更しているからなのか、ここでflushしないとdeleteが実施されずduplicate keyエラーになる。
        repo.flush();
      }

      // 同一Unique Keyで削除済みのレコード存在チェック。あれば削除
      if (entity.hasNaturalKey()) {
        result = repo.findByNaturalKeyAndSoftDeleteFieldTrueFromAllGroups(entity);
        if (result.isPresent()) {
          repo.delete(result.get());
          // sessionのfilterを変更しているからなのか、ここでflushしないとdeleteが実施されずduplicate keyエラーになる。
          repo.flush();
        }
      }
    }
  }

  protected void enableAllFilters(Object groupId) {
    filterUtil.enableSoftDeleteFilter();

    if (groupId != null) {
      filterUtil.enableGroupFilter(groupId);
    }
  }

  protected void disableAllFilters(Object groupId) {
    filterUtil.disableSoftDeleteFilter();

    if (groupId != null) {
      filterUtil.disableGroupFilter();
    }
  }
}
