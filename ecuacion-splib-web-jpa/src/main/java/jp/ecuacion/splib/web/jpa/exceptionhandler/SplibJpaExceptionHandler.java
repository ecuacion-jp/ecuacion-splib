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
package jp.ecuacion.splib.web.jpa.exceptionhandler;

import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.splib.web.exceptionhandler.SplibExceptionHandler;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/** SplibExceptionHandlerNoJpaに、jpa独自の例外処理を追加したクラス。JPAを使用している場合はこちらを継承。 */
public abstract class SplibJpaExceptionHandler extends SplibExceptionHandler {

  /**
   * 楽観的排他制御の処理。 画面表示〜ボタン押下の間にレコード更新された場合は、手動でチェックなので直接BizLogicAppExceptionを投げても良いのだが、
   * 複数sessionで同一レコードを同時更新した場合（service内の処理内でのselectからupdateの間に別sessionがselect〜updateを完了）は
   * JPAが自動でObjectOptimisticLockingFailureExceptionを投げてくるので、それも同様に処理できるよう楽観的排他制御エラーは
   * ObjectOptimisticLockingFailureExceptionで統一しておく。
   * 尚、jpa以外でも、jdbcやfileでのlockでも同様の事象があるので全て統一しObjectOptimisticLockingFailureExceptionを使用するものとする。
   */
  @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
  public ModelAndView handleObjectOptimisticLockingFailureException(
      ObjectOptimisticLockingFailureException exception,
      @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    return super.handleOptimisticLockingFailureException(null, loginUser);
  }

  @ExceptionHandler({PessimisticLockingFailureException.class})
  public ModelAndView handlePessimisticLockingFailureException(
      PessimisticLockingFailureException exception, @AuthenticationPrincipal UserDetails loginUser)
      throws Exception {
    // 通常のチェックエラー扱いとする
    return handleAppException(
        new BizLogicAppException("jp.ecuacion.splib.web.common.message.pessimisticLocking"),
        loginUser);
  }
}
