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
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jp.ecuacion.lib.jpa.entity.AbstractEntity;
import jp.ecuacion.splib.core.container.DatetimeFormatParameters;
import jp.ecuacion.splib.web.form.SplibListForm;
import jp.ecuacion.splib.web.form.SplibSearchForm;
import jp.ecuacion.splib.web.service.SplibSearchListService;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides abstract search list service.
 * 
 * @param <FST> SplibEditForm
 * @param <FLT> SplibEditForm
 * @param <E> AbstractEntity
 */
//@formatter:off
@Transactional(rollbackFor = Exception.class)
public abstract class SplibSearchListJpaService<FST extends SplibSearchForm, 
    FLT extends SplibListForm<?>, E extends AbstractEntity>
    extends SplibSearchListService<FST, FLT> implements SplibJpaServiceInterface<E> {
  //@formatter:on

  @Autowired
  private HttpServletRequest request;

  protected EntityManager em;

  //
  // common
  //

  /**
   * offsetはlogin画面でのonload時に呼ばれるため、login画面を開いた状態で放置した場合は値がnullでエラーになる。
   */
  public DatetimeFormatParameters getParams() {
    return new SplibUtil().getParams(request);
  }

  /**
   * record内のlocalDate項目（String）をLocalDate形式で取得。
   */
  protected LocalDate localDate(String date) {
    return (date == null || date.equals("")) ? null
        : LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  public void setEntityManager(EntityManager em) {
    this.em = em;
  }

  protected abstract Specification<E> getSpecs(FST searchForm);

  protected Page<E> getListFormCommon(FST searchForm,
      JpaSpecificationExecutor<E> repository) {
    Specification<E> specs = getSpecs(searchForm);

    // 指定条件での検索結果の全件数を取得。併せてページ調整
    searchForm.setNumberOfRecordsAndAdjustCurrentPageNumger(repository.count(specs));

    // ここでは指定ページ分のレコードのみ取得
    return repository.findAll(specs, searchForm.getPageRequest());
  }
}
