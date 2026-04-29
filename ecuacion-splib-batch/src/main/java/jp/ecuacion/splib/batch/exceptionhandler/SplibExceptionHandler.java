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
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.splib.batch.advice.SplibBatchAdvice;
import jp.ecuacion.splib.core.exceptionhandler.SplibExceptionHandlerAction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.ExceptionHandler;
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

  @Nullable
  private SplibExceptionHandlerAction action;

  /**
   * Constructs a new instance.
   *
   * @param action action, may be {@code null}
   */
  public SplibExceptionHandler(@Nullable SplibExceptionHandlerAction action) {
    this.action = action;
  }

  private DetailLogger detailLog = new DetailLogger(this);

  private static final String NOT_FOUND_MSG_TMPL =
      "(The classname of {0} is null because the error occurred outside {0}"
          + "or the advice to register current {0} is not implemented.)";

  @Override
  public void handleException(@Nullable RepeatContext context, @Nullable Throwable throwable)
      throws Throwable {

    throwable = ObjectsUtil.requireNonNull(throwable);
    StringBuilder sb = new StringBuilder();
    sb.append(formatMsg("job", SplibBatchAdvice.getCurrentJob(), true));
    sb.append(formatMsg("step", SplibBatchAdvice.getCurrentStep(), false));
    sb.append(formatMsg("tasklet or chunk", SplibBatchAdvice.getCurrentTaskletOrChunk(), false));
    detailLog.info(sb.toString());

    LogUtil.logSystemError(detailLog, throwable);

    if (action != null) {
      try {
        Objects.requireNonNull(action).execute(throwable);

      } catch (Throwable th) {
        LogUtil.logSystemError(detailLog, th);
      }
    }

    // For ViolationException, list all messages so they are visible in the log.
    if (throwable instanceof ViolationException) {
      List<@NonNull String> msgList = ExceptionUtil.getMessageList(
          (ViolationException) throwable, Locale.getDefault());
      detailLog.info("==========");
      detailLog.info("[ViolationException message list]");
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
