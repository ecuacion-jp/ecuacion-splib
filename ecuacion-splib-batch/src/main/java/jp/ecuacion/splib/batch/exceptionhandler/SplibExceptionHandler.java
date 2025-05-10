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
package jp.ecuacion.splib.batch.exceptionhandler;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.annotation.RequireNonnull;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.unchecked.UncheckedAppException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.splib.batch.advice.SplibBatchAdvice;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides {@code ExceptionHandler}.
 * 
 * <p>In batch apps, this class is always used 
 *     while in web apps the use of {@code SplibExceptionHandler} is arbitrary.<br>
 *     It seems that in batch apps that versatility is not needed.</p>
 */
@Component
public class SplibExceptionHandler implements ExceptionHandler {

  @Autowired(required = false)
  private SplibExceptionHandlerAction action;

  private DetailLogger detailLog = new DetailLogger(this);

  private static final String NOT_FOUND_MSG_TMPL =
      "(The classname of {0} is null because the error occurred outside {0}"
          + "or the advice to register current {0} is not implemented.)";

  @Override
  public void handleException(RepeatContext context, @RequireNonnull Throwable throwable)
      throws Throwable {

    throwable = ObjectsUtil.paramRequireNonNull(throwable);
    StringBuilder sb = new StringBuilder();
    sb.append(formatMsg("job", SplibBatchAdvice.getCurrentJob(), true));
    sb.append(formatMsg("step", SplibBatchAdvice.getCurrentStep(), false));
    sb.append(formatMsg("tasklet or chunk", SplibBatchAdvice.getCurrentTaskletOrChunk(), false));

    LogUtil.logSystemError(detailLog, throwable);

    if (action != null) {
      try {
        action.execute(throwable);

      } catch (Throwable th) {
        LogUtil.logSystemError(detailLog, th);
      }
    }

    // appExceptionの場合、特にMultipleAppExceptionで複数のメッセージがある場合、
    // 一覧で見えないとわかりにくいので改めて一覧表示しておく。
    if (throwable instanceof AppException || throwable instanceof UncheckedAppException) {
      AppException appEx = null;
      if (throwable instanceof UncheckedAppException) {
        appEx = ((AppException) ((UncheckedAppException) throwable).getCause());

      } else {
        appEx = (AppException) throwable;
      }

      List<String> msgList = ExceptionUtil.getAppExceptionMessageList(appEx, Locale.getDefault());
      detailLog.info("==========");
      detailLog.info("[AppException メッセージ一覧]");
      msgList.stream().forEach(msg -> detailLog.info(msg));
      detailLog.info("==========");
    }

    throw throwable;
  }

  private String formatMsg(String kind, String name, boolean is1st) {
    return (is1st ? "" : ", ")
        + (name == null ? MessageFormat.format(NOT_FOUND_MSG_TMPL, "'" + kind + "'")
            : kind + ": " + name);
  }
}
