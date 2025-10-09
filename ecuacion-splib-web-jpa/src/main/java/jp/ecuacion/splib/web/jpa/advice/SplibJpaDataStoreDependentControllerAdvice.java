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
package jp.ecuacion.splib.web.jpa.advice;

import jp.ecuacion.splib.core.record.SplibRecord;
import jp.ecuacion.splib.jpa.bean.SplibControllerAdviceInfoBean;
import jp.ecuacion.splib.jpa.util.SplibJpaFilterUtil;
import jp.ecuacion.splib.web.advice.SplibDataStoreDependentControllerAdvice;
import jp.ecuacion.splib.web.service.SplibDataStoreDependentControllerAdviceService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides the function that puts logged-in accounts obtained with JPA info to {@code Model}. 
 * Additionally, login-related procedure is done here.
 * 
 * @see jp.ecuacion.splib.web.advice.SplibDataStoreDependentControllerAdvice
 */
public class SplibJpaDataStoreDependentControllerAdvice
    extends SplibDataStoreDependentControllerAdvice {

  @Autowired
  SplibDataStoreDependentControllerAdviceService service;

  @Autowired
  SplibJpaFilterUtil filterUtil;

  @Override
  protected void executeForAll() {
    // 削除フラグのfilterを設定
    filterUtil.enableSoftDeleteFilter();
  }

  @Override
  protected void executeForAccount(SplibRecord loginAcc) {
    Object accGroupId = service.getGroupId(loginAcc);
    // SoftDeleteAdviceで使用するためgroupIdを保管
    SplibControllerAdviceInfoBean.setGroupId(accGroupId);

    // groupのfilterを設定
    filterUtil.enableGroupFilter(accGroupId);
  }
}
