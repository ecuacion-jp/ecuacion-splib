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
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.annotation.RequireNonnull;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.AppWarningException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.exception.unchecked.EclibRuntimeException;
import jp.ecuacion.lib.core.exception.unchecked.UncheckedAppException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.bean.MessagesBean.WarnMessageBean;
import jp.ecuacion.splib.web.bean.ReturnUrlBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibEditController;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.RedirectException;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.exception.WebAppWarningException;
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
    // 本当は、ExceptionHandlerのエラー処理の中でも、redirectしない場合のみprepareFormを呼べばいいはずなのだが
    // そこのhandlingはまだできていない。必要時に整理。
    getController().getService().prepareForm(Arrays.asList(forms), loginUser);

    // redirectBean
    // redirectBeanがない場合は自画面遷移。この場合は他のmodelの情報も全て遷移先に渡す。
    // 後続処理の簡便化のため、自画面遷移の場合のredirectBeanを生成しておく。
    if (redirectBean == null) {
      redirectBean = new ReturnUrlBean(ctrl, util, false);
    }

    // redirectの有無で分岐
    if (!isRedirect) {
      return new ModelAndView(ctrl.getDefaultHtmlPageName(), getModel().asMap());

    } else {
      // redirectBean == nullの場合は自画面遷移、自画面遷移の場合はmodelの情報も保持する
      String path = util.prepareForPageTransition(request, redirectBean, getModel(), true);

      return new ModelAndView(path);
    }
  }

  /**
   * Catches {@code AppWarningException}.
   * 
   * @param exception AppWarningException
   * @param loginUser UserDetails, may be {@code null} when the user is not logged in
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({AppWarningException.class})
  public @Nonnull ModelAndView handleAppWarningException(@Nonnull WebAppWarningException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {
    MessagesBean messagesBean =
        ((MessagesBean) getModel().getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));

    Objects.requireNonNull(messagesBean);
    messagesBean.setWarnMessage(new WarnMessageBean(
        exception.getMessageId(), PropertyFileUtil.getMessage(request.getLocale(),
            exception.getMessageId(), exception.getMessageArgs()),
        exception.buttonIdToPressOnConfirm()));

    // warningはsubmitが完了していないことから同じ画面に戻る処理なので、別ページへのredirectは発生しない
    // PRGのためisRedirectはtrueとしておく
    return appExceptionFinalHandler(getController(), loginUser, false, null);
  }

  /**
   * Catches {@code AppException}.
   * 
   * @param exception AppException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({AppException.class})
  public @Nonnull ModelAndView handleAppException(@Nonnull AppException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    // MultipleAppExceptionも考慮し例外を複数持つ
    List<SingleAppException> exList = new ArrayList<>();

    if (exception instanceof MultipleAppException) {
      for (SingleAppException appEx : ((MultipleAppException) exception).getList()) {
        exList.add(appEx);
      }

      // 結果exListがゼロの場合はシステムエラーとして処理。
      if (exList.size() == 0) {
        throw new RuntimeException("No exception included in MultipleAppException.");
      }

    } else {
      exList.add((SingleAppException) exception);
    }

    MessageSetter msgSetter = new MessageSetter();
    MessagesBean messagesBean =
        ((MessagesBean) getModel().getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));

    boolean hasDesignatedItemPropertyPathInBizLogicAppException = false;

    // Process AppExceptions one by one in MultipleAppException
    for (SingleAppException saex : exList) {

      List<String> recordPropertyPathList = new ArrayList<>();
      for (String itemPropertyPath : saex.getItemPropertyPaths()) {
        if (!itemPropertyPath.startsWith(getController().getRootRecordName() + ".")) {
          recordPropertyPathList.add(getController().getRootRecordName() + "." + itemPropertyPath);

        } else {
          recordPropertyPathList.add(itemPropertyPath);
        }
      }

      String[] recordPropertyPaths =
          recordPropertyPathList.toArray(new String[recordPropertyPathList.size()]);

      msgSetter.setMessage(messagesBean, saex, request.getLocale(), recordPropertyPaths);

      if (saex instanceof BizLogicAppException) {
        String[] paths = ((BizLogicAppException) saex).getItemPropertyPaths();
        if (paths != null && paths.length > 0) {
          hasDesignatedItemPropertyPathInBizLogicAppException = true;
        }
      }
    }

    msgSetter.addMessageThatSaysThereIsAnError(messagesBean, request.getLocale(),
        hasDesignatedItemPropertyPathInBizLogicAppException);

    ReturnUrlBean redirectBean = getController().getRedirectUrlOnAppExceptionBean();
    return appExceptionFinalHandler(getController(), loginUser, true, redirectBean);
  }

  /**
   * Catches {@code UncheckedAppException}.
   * 
   * @param exception UncheckedAppException
   * @param loginUser UserDetails
   * @return ModelAndView
   * @throws Exception Exception
   */
  @ExceptionHandler({UncheckedAppException.class})
  public @Nonnull ModelAndView handleUncheckedAppException(@Nonnull UncheckedAppException exception,
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    // Treat as normal BizLogicAppException.
    return handleAppException((AppException) exception.getCause(), loginUser);
  }

  /**
   * Catches {@code OverlappingFileLockException}.
   * 
   * @param exception AppException
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
      // Treat as normal BizLogicAppException
      return handleAppException(new BizLogicAppException(msgId), loginUser);
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
  public @Nonnull ModelAndView handleRedirectNeededExceptions(@RequireNonnull Exception exception,
      Model newModel) {
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

      // Change exception into BizLogicAppException to let MessageSetter to accept the exception.
      BizLogicAppException blaex = new BizLogicAppException(redirectException.getMessageId(),
          redirectException.getMessageArgs());
      new MessageSetter().setMessage(messagesBean, blaex, request.getLocale());
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

  private static class MessageSetter {

    private static boolean needsMsgAtItem = Boolean.valueOf(PropertyFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.process-result-message.shown-at-each-item", "false"));
    private static boolean needsMsgAtTop = Boolean.valueOf(PropertyFileUtil.getApplicationOrElse(
        "jp.ecuacion.splib.web.process-result-message.shown-at-the-top", "false"));

    public void setMessage(MessagesBean messagesBean, SingleAppException saex, Locale locale,
        String... recordPropertyPaths) {

      // Throw an exception when both needsMsgAtItem and needsMsgAtTop are false.
      if (!needsMsgAtItem && !needsMsgAtTop) {
        String msg = "One of 'jp.ecuacion.splib.web.process-result-message.shown-at-each-item' or "
            + "'jp.ecuacion.splib.web.process-result-message.shown-at-the-top' must be true.";
        throw new EclibRuntimeException(msg);
      }

      // message
      String messageAtItem = null;
      String messageAtTop = null;
      if (needsMsgAtItem) {
        messageAtItem = ExceptionUtil.getAppExceptionMessageList(saex, locale, false).get(0);

        // showsAtEachItem == false even if needsMsgAtItem == true
        // when recordPropertyPaths.length == 0
        messagesBean.setErrorMessage(messageAtItem, recordPropertyPaths.length != 0,
            recordPropertyPaths);
      }

      if (needsMsgAtTop) {
        messageAtTop = ExceptionUtil.getAppExceptionMessageList(saex, locale, true).get(0);

        // Skip the message when needsMsgAtItem is also true and recordPropertyPaths.length == 0
        // because it's exactly the same as the one with needsMsgAtItem
        // (Technically the message string can be changed but it is supposed to mean the same thing)
        if (!(needsMsgAtItem && recordPropertyPaths.length == 0)) {
          messagesBean.setErrorMessage(messageAtTop, false, recordPropertyPaths);
        }
      }
    }

    public void addMessageThatSaysThereIsAnError(MessagesBean messagesBean, Locale locale,
        boolean hasDesignatedItemPropertyPathInBizLogicAppException) {
      if (needsMsgAtItem && !needsMsgAtTop && hasDesignatedItemPropertyPathInBizLogicAppException) {
        String key = "jp.ecuacion.splib.web.common.message.messagesLinkedToItemsExist";
        messagesBean.setErrorMessage(PropertyFileUtil.getMessage(locale, key), false);
      }
    }
  }
}
