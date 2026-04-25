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
package jp.ecuacion.splib.web.exceptionhandler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.Locale;
import jp.ecuacion.lib.core.exception.ConstraintViolationExceptionWithParameters;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.exception.ViolationWarningException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.core.violation.Violations.MessageParameters;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.bean.MessagesBean.WarnMessageBean;
import jp.ecuacion.splib.web.bean.ReturnUrlBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibEditController;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.RedirectException;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Provides an exception handler.
 */
public abstract class SplibExceptionHandler {

  private DetailLogger detailLog = new DetailLogger(this);

  @Autowired
  HttpServletRequest request;

  @Autowired(required = false)
  SplibExceptionHandlerAction actionOnThrowable;

  @Autowired
  private SplibUtil util;

  /**
   * Returns the controller from which the exception throws.
   *
   * @return SplibGeneralController
   */
  @Nonnull
  protected SplibGeneralController<?> getController() {
    return (SplibGeneralController<?>) getModel().getAttribute(SplibWebConstants.KEY_CONTROLLER);
  }

  /**
   * Returns the forms from which the exception throws.
   *
   * @return SplibGeneralController
   */
  @Nonnull
  private SplibGeneralForm[] getForms() {
    return ((SplibGeneralForm[]) getModel().getAttribute(SplibWebConstants.KEY_FORMS));
  }

  /**
   * Returns the model obtained at the controller.
   *
   * @return Model
   */
  @Nonnull
  private Model getModel() {
    return (Model) request.getAttribute(SplibWebConstants.KEY_MODEL);
  }

  private @Nonnull ModelAndView appExceptionFinalHandler(@Nonnull SplibGeneralController<?> ctrl,
      @Nullable UserDetails loginUser, boolean isRedirect, @Nullable ReturnUrlBean redirectBean) {

    SplibGeneralForm[] forms =
        (SplibGeneralForm[]) getModel().getAttribute(SplibWebConstants.KEY_FORMS);

    // #603:
    // Ideally prepareForm should only be called when not redirecting within the ExceptionHandler,
    // but that handling is not yet implemented. To be refactored when needed.
    getController().getService().prepareForm(Arrays.asList(forms), loginUser);

    // redirectBean
    // If redirectBean is null, transition to the same page.
    // In that case all model information is also passed to the destination.
    // To simplify subsequent processing, generate a redirectBean for same-page transitions.
    if (redirectBean == null) {
      redirectBean = new ReturnUrlBean(ctrl, util, false);
    }

    // Branch on whether redirect is needed.
    if (!isRedirect) {
      return new ModelAndView(ctrl.getDefaultHtmlPageName(), getModel().asMap());

    } else {
      // When redirectBean is null it is a same-page transition; in that case model info is kept.
      String path = util.prepareForPageTransition(request, redirectBean, getModel(), true);

      return new ModelAndView(path);
    }
  }

  /**
   * Catches {@code ViolationWarningException}.
   *
   * @param exception ViolationWarningException
   * @param loginUser UserDetails, may be {@code null} when the user is not logged in
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({ViolationWarningException.class})
  public @Nonnull ModelAndView handleWarning(@Nonnull ViolationWarningException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    MessagesBean messagesBean =
        ((MessagesBean) getModel().getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));

    BusinessViolation v = exception.getViolations().getBusinessViolations().get(0);
    String buttonId = exception instanceof ViolationWebWarningException
        ? ((ViolationWebWarningException) exception).getButtonIdPressedIfConfirmed() : null;
    messagesBean.setWarnMessage(new WarnMessageBean(
        v.getMessageId(), PropertiesFileUtil.getMessage(request.getLocale(),
            v.getMessageId(), v.getMessageArgs()),
        buttonId));

    // Since warning means the submit did not complete, processing returns to the same page,
    // so no redirect to a different page occurs.
    // isRedirect is set to true for PRG.
    return appExceptionFinalHandler(getController(), loginUser, false, null);
  }

  /**
   * Catches {@code ViolationException}.
   *
   * @param exception ViolationException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({ViolationException.class})
  public @Nonnull ModelAndView handleViolationException(@Nonnull ViolationException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    boolean needsMsgAtItemDefault = Boolean.valueOf(PropertiesFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.process-result-message.shown-at-each-item", "false"));
    boolean needsMsgAtTopDefault = Boolean.valueOf(PropertiesFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.process-result-message.shown-at-the-top", "false"));

    MessagesBean messagesBean =
        (MessagesBean) getModel().getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN);

    Violations violations = exception.getViolations();
    MessageParameters params = violations.messageParameters();

    for (ConstraintViolation<?> cv : violations.getConstraintViolations()) {
      boolean needsMsgAtItem = !needsMsgAtItemDefault ? false
          : (params.isMessageWithItemName() != null ? !params.isMessageWithItemName() : true);
      boolean needsMsgAtTop = !needsMsgAtItem;
      String message = ExceptionUtil.getMessageList(
          new Violations().messageParameters(params).add(cv), request.getLocale(),
          needsMsgAtTop).get(0);
      if (needsMsgAtItem) {
        if (messagesBean.getErrorMessagesAtEachItem().size() == 0) {
          String key = "jp.ecuacion.splib.web.common.message.messagesLinkedToItemsExist";
          messagesBean.setErrorMessage(PropertiesFileUtil.getMessage(request.getLocale(), key),
              false);
        }
        messagesBean.setErrorMessage(message, true, new String[] {});
      } else {
        messagesBean.setErrorMessage(message, false, new String[] {});
      }
    }

    for (BusinessViolation bv : violations.getBusinessViolations()) {
      setMessage(messagesBean, bv, needsMsgAtItemDefault, needsMsgAtTopDefault,
          request.getLocale());
    }

    ReturnUrlBean redirectBean = getController().getRedirectUrlOnAppExceptionBean();
    return appExceptionFinalHandler(getController(), loginUser, true, redirectBean);
  }

  /**
   * Catches {@code ConstraintViolationException}.
   *
   * @param exception ConstraintViolationException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({ConstraintViolationException.class})
  public @Nonnull ModelAndView handleConstraintViolationException(
      @Nonnull ConstraintViolationException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    MessageParameters params = exception instanceof ConstraintViolationExceptionWithParameters
        ? ((ConstraintViolationExceptionWithParameters) exception).getMessageParameters()
        : new MessageParameters();
    Violations violations = new Violations().messageParameters(params);
    for (ConstraintViolation<?> cv : exception.getConstraintViolations()) {
      violations.add(cv);
    }
    return handleViolationException(new ViolationException(violations), loginUser);
  }

  /**
   * Catches {@code OverlappingFileLockException}.
   *
   * @param exception OverlappingFileLockException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({OverlappingFileLockException.class})
  public @Nonnull ModelAndView handleOptimisticLockingFailureException(
      @Nonnull OverlappingFileLockException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser, Model model) throws Exception {

    String msgId = "jp.ecuacion.splib.web.common.message.optimisticLocking";
    if (getController() instanceof SplibEditController) {
      String loginState = (String) getModel().getAttribute("loginState");
      String path = "/" + loginState + "/" + getController().getFunction() + "/"
          + getController().getDefaultDestSubFunctionOnNormalEnd() + "/"
          + getController().getDefaultDestPageOnNormalEnd();
      return handleRedirectNeededExceptions(new RedirectException(path, msgId), model);
    } else {
      Violations v = new Violations();
      v.add(new BusinessViolation(msgId));
      return handleViolationException(new ViolationException(v), loginUser);
    }
  }

  /**
   * Catches some specific exceptions.
   *
   * <ul>
   * <li>NoResourceFoundException:
   * No @RequestMapping settings in controllers which matches the request url.</li>
   * <li>RedirectException: @RequestMapping settings
   * exists, but html file does not exist.</li>
   * </ul>
   *
   * @param exception Exception
   * @param newModel When Exception occurs before Controller#prepare called, getModel() is null.
   *     In that case, this new model can be used.
   *     This is different from the one you get at controller.
   * @return ModelAndView
   */
  @ExceptionHandler({NoResourceFoundException.class, RedirectException.class})
  public @Nonnull ModelAndView handleRedirectNeededExceptions(Exception exception, Model newModel) {
    RedirectException redirectException = null;

    // Setup model if it's new.
    Model model = getModel();
    if (model == null) {
      model = newModel;
      newModel.addAttribute(SplibWebConstants.KEY_MESSAGES_BEAN, new MessagesBean());
    }

    if (!StringUtils.isEmpty(exception.getMessage())) {
      detailLog.info(exception.getMessage());
    }

    if (exception instanceof NoResourceFoundException) {
      String path = ((NoResourceFoundException) exception).getResourcePath();

      redirectException = new RedirectToHomePageException(
          "jp.ecuacion.splib.web.common.message.NoResourceFoundException", path);

    } else {
      redirectException = (RedirectException) exception;
    }

    // Logging
    if (redirectException.getLogLevel() != null) {
      detailLog.log(redirectException.getLogLevel(), redirectException.getLogString());
    }

    // Showing message
    if (!StringUtils.isEmpty(redirectException.getMessageId())) {
      MessagesBean messagesBean =
          ((MessagesBean) model.getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));

      setMessage(messagesBean,
          new BusinessViolation(redirectException.getMessageId(),
              redirectException.getMessageArgs()),
          false, true, request.getLocale());
    }

    // redirect
    ReturnUrlBean redirectBean = new ReturnUrlBean(redirectException.getRedirectPath());
    String path = util.prepareForPageTransition(request, redirectBean, model, true);
    return new ModelAndView(path);
  }

  /**
   * Catches {@code Throwable}.
   *
   * @param exception Throwable
   * @param model model
   * @return ModelAndView
   */
  @ExceptionHandler({Throwable.class})
  public @Nonnull ModelAndView handleThrowable(@Nonnull Throwable exception, @Nonnull Model model) {

    LogUtil.logSystemError(detailLog, exception);

    // app dependent procedures, like sending mail.
    if (actionOnThrowable != null) {
      actionOnThrowable.execute(exception);
    }

    Model mdl = getModel() == null ? model : getModel();
    return new ModelAndView("error", mdl.asMap(), HttpStatusCode.valueOf(500));
  }

  private void setMessage(MessagesBean messagesBean, BusinessViolation violation,
      boolean needsMsgAtItemAsDefault, boolean needsMsgAtTopAsDefault, Locale locale) {

    if (!needsMsgAtItemAsDefault && !needsMsgAtTopAsDefault) {
      String msg =
          "One of 'jp.ecuacion.splib.web.process-result-message.shown-at-each-item' or "
              + "'jp.ecuacion.splib.web.process-result-message.shown-at-the-top' must be true.";
      throw new RuntimeException(msg);
    }

    boolean needsMsgAtItem =
        !needsMsgAtItemAsDefault ? false : violation.getItemPropertyPaths().length > 0;
    boolean needsMsgAtTop = !needsMsgAtItem;

    String message = ExceptionUtil
        .getMessageList(new Violations().add(violation), locale, needsMsgAtTop).get(0);

    if (needsMsgAtItem) {
      if (messagesBean.getErrorMessagesAtEachItem().size() == 0) {
        String key = "jp.ecuacion.splib.web.common.message.messagesLinkedToItemsExist";
        messagesBean.setErrorMessage(PropertiesFileUtil.getMessage(locale, key), false);
      }
      messagesBean.setErrorMessage(message, true, violation.getItemPropertyPaths());
    }

    if (needsMsgAtTop) {
      messagesBean.setErrorMessage(message, false, violation.getItemPropertyPaths());
    }
  }
}
