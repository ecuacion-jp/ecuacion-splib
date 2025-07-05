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
import jp.ecuacion.splib.web.exception.BizLogicRedirectAppException;
import jp.ecuacion.splib.web.exception.HtmlFileNotAllowedToOpenException;
import jp.ecuacion.splib.web.exception.HtmlFileNotFoundException;
import jp.ecuacion.splib.web.exception.WebAppWarningException;
import jp.ecuacion.splib.web.form.SplibGeneralForm;
import jp.ecuacion.splib.web.form.record.RecordInterface;
import jp.ecuacion.splib.web.util.SplibUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides an exception handler.
 */
public abstract class SplibExceptionHandler {

  private DetailLogger detailLog = new DetailLogger(this);

  @Autowired
  HttpServletRequest request;

  @Autowired(required = false)
  SplibExceptionHandlerAction actionOnThrowable;

  // @Autowired
  // private SplibModelAttributes modelAttr;

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
   * "{0}"がmessageに含まれる場合はitemName（複数項目ある場合は複数項目）で置き換える。
   */
  @Nonnull
  private String addItemDisplayNames(@RequireNonnull String message,
      @Nonnull String... itemKindIds) {
    if (message.contains("{0}")) {
      message = MessageFormat.format(message,
          getItemDisplayNames(ObjectsUtil.requireSizeNonZero(itemKindIds)));
    }

    return message;
  }

  @Nonnull
  private String getItemDisplayNames(@RequireNonnull String[] itemKindIds) {
    StringBuilder sb = new StringBuilder();
    final String prependParenthesis = PropertyFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.prependParenthesis");
    final String appendParenthesis = PropertyFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.appendParenthesis");
    final String separator = PropertyFileUtil.getMessage(request.getLocale(),
        "jp.ecuacion.splib.web.common.message.itemName.separator");

    boolean is1stTime = true;
    for (String itemKindId : ObjectsUtil.requireNonNull(itemKindIds)) {

      // itemNameがmessages.propertiesにあったらそれに置き換える
      if (PropertyFileUtil.hasItemName(itemKindId)) {
        itemKindId = PropertyFileUtil.getItemName(request.getLocale(), itemKindId);
      }

      if (is1stTime) {
        is1stTime = false;

      } else {
        sb.append(separator);
      }

      sb.append(prependParenthesis + itemKindId + appendParenthesis);
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
      String path = util.prepareForPageTransition(request, ctrl, redirectBean, getModel(), true);

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

    ReturnUrlBean redirectBean = null;
    MessagesBean messagesBean =
        ((MessagesBean) getModel().getAttribute(SplibWebConstants.KEY_MESSAGES_BEAN));

    // BizLogicRedirectAppException
    if (exception instanceof BizLogicRedirectAppException) {
      String redirectPath = ((BizLogicRedirectAppException) exception).getRedirectPath();
      redirectBean = new ReturnUrlBean(getController(), redirectPath);

    } else {
      // other than BizLogicRedirectAppException

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

      // exList内のexceptionを一つずつ処理
      for (SingleAppException saex : exList) {

        // propertyPaths
        List<String> propertyPathList = Arrays.asList(saex.getItemPropertyPaths()).stream()
            .map(itemKindId -> StringUtils.uncapitalize(itemKindId)).toList();

        // Add rootRecord plus dot(.) if BizLogicAppException
        if (saex instanceof BizLogicAppException && propertyPathList.size() > 0
            && !propertyPathList.get(0).startsWith(getController().getRootRecordName() + ".")) {
          propertyPathList = propertyPathList.stream()
              .map(path -> getController().getRootRecordName() + "." + path).toList();
        }

        String[] propertyPaths = propertyPathList.toArray(new String[propertyPathList.size()]);

        // message
        Boolean msgAtItem =
            (Boolean) getModel().getAttribute(SplibWebConstants.KEY_MODEL_MESSAGES_AT_ITEMS);
        boolean needsItemName = msgAtItem == null ? true : !msgAtItem;
        String message = ExceptionUtil
            .getAppExceptionMessageList(saex, request.getLocale(), needsItemName).get(0);

        if (saex instanceof ValidationAppException) {
          // messageは既にmessage.propertiesのメッセージを取得し、パラメータも埋めた状態だが、
          // それでも{0}が残っている場合はfieldsの値を元に項目名を埋める。
          // BizLogicAppExceptionの場合はこのロジックに入らず「{0}」のメッセージがそのまま出てもらって構わない
          // （システムエラーになるのは微妙）のでValidationAppExceptionに限定する。
          List<String> itemNameKeyList = new ArrayList<>();
          for (String propertyPath : ObjectsUtil.requireNonNull(propertyPaths)) {
            HtmlItem item =
                ((RecordInterface) getForms()[0].getRootRecord(getController().getRootRecordName()))
                    .getHtmlItem(getController().getRootRecordName(), propertyPath);
            itemNameKeyList.add(item.getItemNameKey(getController().getRootRecordName()));
          }

          message = addItemDisplayNames(message,
              itemNameKeyList.toArray(new String[itemNameKeyList.size()]));
        }

        messagesBean.setErrorMessage(message, propertyPaths);
      }
    }

    redirectBean =
        redirectBean == null ? getController().getRedirectUrlOnAppExceptionBean() : redirectBean;
    return appExceptionFinalHandler(getController(), loginUser, true, redirectBean);
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
    // 通常のチェックエラー扱いとする
    return handleAppException(
        new BizLogicAppException("jp.ecuacion.splib.web.common.message.optimisticLocking"),
        loginUser);
  }

  /**
   * Catches some specific exceptions. 
   * 
   * @param exception Exception
   * @return ModelAndView
   */
  @ExceptionHandler({HtmlFileNotFoundException.class, HtmlFileNotAllowedToOpenException.class})
  public @Nonnull ModelAndView handleExceptionsWhichLeadsToDefaultPage(
      @Nonnull Exception exception) {

    detailLog.info(exception.getMessage());

    // Output log to error.log
    if (exception instanceof HtmlFileNotAllowedToOpenException) {
      detailLog.error(exception);
    }

    // Redirect to a page which seems to exist.
    // If it does not, it will be re-redirected to an existent page.
    return new ModelAndView("redirect:/public/home/page");
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

    // 個別appの処理。mail送信など。
    if (actionOnThrowable != null) {
      actionOnThrowable.execute(exception);
    }

    return new ModelAndView("redirect:/"
        + PropertyFileUtil.getApplication("jp.ecuacion.splib.web.system-error.go-to-path"));

    // Model mdl = getModel() == null ? model : getModel();
    // modelAttr.addAllToModel(mdl);
    //
    // return new ModelAndView("error", mdl.asMap(), HttpStatusCode.valueOf(500));
  }
}
