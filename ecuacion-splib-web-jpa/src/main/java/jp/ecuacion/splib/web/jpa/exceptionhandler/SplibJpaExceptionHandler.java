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

import jakarta.servlet.http.HttpServletRequest;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.web.exceptionhandler.SplibExceptionHandler;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides JPA related exception handling 
 * in addition to the handling within {@code SplibExceptionHandler}.
 * 
 * <p>Use this class when the app uses JPA.</p>
 */
public abstract class SplibJpaExceptionHandler extends SplibExceptionHandler {

  /**
   * Constructs a new instance.
   *
   * @param request request
   * @param actionOnThrowable actionOnThrowable, may be {@code null}
   * @param util util
   */
  protected SplibJpaExceptionHandler(HttpServletRequest request,
      @Nullable SplibExceptionHandlerAction actionOnThrowable, SplibUtil util) {
    super(request, actionOnThrowable, util);
  }

  /**
   * Catches {@code ObjectOptimisticLockingFailureException} 
   * and treats the optimistic exclusive controls.
   * 
   * <p>Optimistic exclusive control causes an exception when a record is updated 
   *     in the interval between pressing the button and actually updating the record (A).<br>
   *     But usually optimistic exclusive control is also needed when a record is updated 
   *     in the interval between displaying the screen and pressing the button (B), 
   *     and in the interval between displaying the record in the list page 
   *     and displaying the detail page by selecting the record in the list page (C).</p>
   *  
   *  <p>(A) is implemented by JPA, and(A) throws
   *      {@code ObjectOptimisticLockingFailureException}.<br>
   *      (A) automatically works so app developer don't have to cares about it.
   *      On the other hand (B) and (C) is not implemented by JPA, 
   *      so it's implemented in {@code ecuacion-splib}.<br>
   *      But in {@code ecuacion-splib} (B) and (C) 
   *      also throws {@code ObjectOptimisticLockingFailureException} for integrated handling.</p>
   */
  @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
  public ModelAndView handleObjectOptimisticLockingFailureException(
      ObjectOptimisticLockingFailureException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser, Model model) throws Exception {
    return super.handleOptimisticLockingFailureException(null, loginUser, model);
  }

  /**
   * Catches {@code PessimisticLockingFailureException}.
   * 
   * @param exception exception
   * @param loginUser loginUser
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({PessimisticLockingFailureException.class})
  public ModelAndView handlePessimisticLockingFailureException(
      PessimisticLockingFailureException exception, @AuthenticationPrincipal UserDetails loginUser)
      throws Exception {
    Violations v = new Violations();
    v.add(new BusinessViolation("jp.ecuacion.splib.web.common.message.pessimisticLocking"));
    return handleViolationException(new ViolationException(v), loginUser);
  }
}
