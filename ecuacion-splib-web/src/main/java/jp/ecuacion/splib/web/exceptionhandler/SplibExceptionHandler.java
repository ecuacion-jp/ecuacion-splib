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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jp.ecuacion.lib.core.annotation.RequireNonnull;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.AppWarningException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.exception.checked.ValidationAppException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.lib.core.util.PropertyFileUtil;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import jp.ecuacion.splib.web.bean.HtmlItem;
import jp.ecuacion.splib.web.bean.MessagesBean;
import jp.ecuacion.splib.web.bean.MessagesBean.WarnMessageBean;
import jp.ecuacion.splib.web.bean.ReturnUrlBean;
import jp.ecuacion.splib.web.constant.SplibWebConstants;
import jp.ecuacion.splib.web.controller.SplibGeneralController;
import jp.ecuacion.splib.web.exception.RedirectException;
import jp.ecuacion.splib.web.exception.RedirectToHomePageException;
import jp.ecuacion.splib.web.exception.WebAppWarningException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.form.record.RecordInterface;
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

  /*
   * Replace "{0}" in a message with item names.
   */
  @Nonnull
  private String addItemDisplayNames(@RequireNonnull String message,
      @Nonnull String... itemNameKeys) {
    if (message.contains("{0}")) {
      message = MessageFormat.format(message,
          getItemDisplayNames(ObjectsUtil.requireSizeNonZero(itemNameKeys)));
    }

    return message;
  }

  @Nonnull
  private String getItemDisplayNames(@RequireNonnull String[] itemNameKeys) {
    StringBuilder sb = new StringBuilder();
    final String prependParenthesis = PropertyFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.prependParenthesis");
    final String appendParenthesis = PropertyFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.appendParenthesis");
    final String separator = PropertyFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.separator");

    boolean is1stTime = true;
    for (String itemNameKey : ObjectsUtil.requireNonNull(itemNameKeys)) {

      // Replace itemNameKey with itemName if itemNameKey exists in messages.properties.
      if (PropertyFileUtil.hasItemName(itemNameKey)) {
        itemNameKey = PropertyFileUtil.getItemName(request.getLocale(), itemNameKey);
      }

      if (is1stTime) {
        is1stTime = false;

      } else {
        sb.append(separator);
      }

      sb.append(prependParenthesis + itemNameKey + appendParenthesis);
    }

    return sb.toString();
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

    MessagesBean messagesBean =
        ((MessagesBean) getModel().getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));

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

    // Process AppExceptions one by one in MultipleAppException
    for (SingleAppException saex : exList) {

      // // propertyPaths
      // List<String> itemPropertyPathList = Arrays.asList(saex.getItemPropertyPaths()).stream()
      // .map(itemPropertyPath -> StringUtils.uncapitalize(itemPropertyPath)).toList();

      // Add rootRecord plus dot(.) if BizLogicAppException
      List<String> recordPropertyPathList = new ArrayList<>();
      if (saex instanceof BizLogicAppException) {
        for (String itemPropertyPath : saex.getItemPropertyPaths()) {
          if (!itemPropertyPath.startsWith(getController().getRootRecordName() + ".")) {
            recordPropertyPathList
                .add(getController().getRootRecordName() + "." + itemPropertyPath);
          }
        }
      }

      String[] itemPropertyPaths =
          recordPropertyPathList.toArray(new String[recordPropertyPathList.size()]);

      // message
      Boolean msgAtItem = Boolean.valueOf(PropertyFileUtil
          .getApplication("jp.ecuacion.splib.web.process-result-message.shown-at-each-item"));
      boolean needsItemName = msgAtItem == null ? true : !msgAtItem;
      String message =
          ExceptionUtil.getAppExceptionMessageList(saex, request.getLocale(), needsItemName).get(0);

      if (saex instanceof ValidationAppException) {
        // messageは既にmessage.propertiesのメッセージを取得し、パラメータも埋めた状態だが、
        // それでも{0}が残っている場合はfieldsの値を元に項目名を埋める。
        // BizLogicAppExceptionの場合はこのロジックに入らず「{0}」のメッセージがそのまま出てもらって構わない
        // （システムエラーになるのは微妙）のでValidationAppExceptionに限定する。
        List<String> itemNameKeyList = new ArrayList<>();
        for (String propertyPath : ObjectsUtil.requireNonNull(itemPropertyPaths)) {
          HtmlItem item =
              ((RecordInterface) getForms()[0].getRootRecord(getController().getRootRecordName()))
                  .getHtmlItem(propertyPath);
          itemNameKeyList.add(item.getItemNameKey(getController().getRootRecordName()));
        }

        message = addItemDisplayNames(message,
            itemNameKeyList.toArray(new String[itemNameKeyList.size()]));
      }

      messagesBean.setErrorMessage(message, itemPropertyPaths);
    }

    ReturnUrlBean redirectBean = getController().getRedirectUrlOnAppExceptionBean();
    return

    appExceptionFinalHandler(getController(), loginUser, true, redirectBean);
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
      @Nullable @AuthenticationPrincipal UserDetails loginUser) throws Exception {

    // Treat as normal BizLogicAppException
    return handleAppException(
        new BizLogicAppException("jp.ecuacion.splib.web.common.message.optimisticLocking"),
        loginUser);
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

    if (exception instanceof RedirectException) {
      redirectException = (RedirectToHomePageException) exception;

    } else {
      if (!StringUtils.isEmpty(exception.getMessage())) {
        detailLog.info(exception.getMessage());
      }

      if (exception instanceof NoResourceFoundException) {
        String path = ((NoResourceFoundException) exception).getResourcePath();

        redirectException = new RedirectToHomePageException(
            "jp.ecuacion.splib.web.common.message.NoResourceFoundException", path);

      } else {
        redirectException = new RedirectToHomePageException();
      }
    }

    // Logging
    if (redirectException.getLogLevel() != null) {
      detailLog.log(redirectException.getLogLevel(), redirectException.getLogString());
    }

    // Showing message
    if (!StringUtils.isEmpty(redirectException.getMessageId())) {
      MessagesBean messagesBean =
          ((MessagesBean) model.getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));
      messagesBean.setErrorMessage(PropertyFileUtil.getMessage(request.getLocale(),
          redirectException.getMessageId(), redirectException.getMessageArgs()));
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
}
