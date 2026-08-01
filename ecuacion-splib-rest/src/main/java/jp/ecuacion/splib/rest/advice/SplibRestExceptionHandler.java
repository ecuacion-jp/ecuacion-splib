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
package jp.ecuacion.splib.rest.advice;

import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.splib.core.exceptionhandler.SplibRestExceptionHandlerAction;
import jp.ecuacion.splib.rest.dto.StatusResponse;
import jp.ecuacion.splib.rest.exception.HttpStatusException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Provides ExceptionHandler.
 */
@RestControllerAdvice
public class SplibRestExceptionHandler extends ResponseEntityExceptionHandler {

  private final DetailLogger dtlLogger = new DetailLogger(this);

  @Nullable
  private final SplibRestExceptionHandlerAction actionOnThrowable;

  /**
   * Constructs a new instance.
   *
   * @param actionOnThrowable actionOnThrowable, may be {@code null}
   */
  public SplibRestExceptionHandler(@Nullable SplibRestExceptionHandlerAction actionOnThrowable) {
    this.actionOnThrowable = actionOnThrowable;
  }

  /**
   * Logs every exception {@link ResponseEntityExceptionHandler}'s built-in handling deals with
   * (this includes {@link org.springframework.web.server.ResponseStatusException} and
   * {@link org.springframework.web.ErrorResponseException}, plus e.g.
   * {@code MethodArgumentNotValidException}, {@code HttpMessageNotReadableException} — anything
   * reaching {@code handleException(Exception, WebRequest)}) before producing the response.
   *
   * <p>Without this override, none of those exceptions were logged anywhere: they're handled by
   * the superclass's own {@code @ExceptionHandler} method — which is {@code final}, so an
   * application (or this class) cannot add its own competing handler for the same types — and
   * that bypasses {@link #handleThrowable}, the only place in this class that logs (and fires
   * {@link #actionOnThrowable}). A {@code ResponseStatusException(INTERNAL_SERVER_ERROR, "...")}
   * thrown deliberately by application code to report a config/environment problem (e.g. a
   * script file that doesn't exist) would reach the client's response body just fine, but never
   * appear in the server's own logs.</p>
   *
   * <p>{@link #actionOnThrowable} (e.g. sending an error mail) is deliberately NOT fired here —
   * only {@link #handleThrowable} does that, for exceptions the application never anticipated at
   * all. Every exception handled here was, by definition, already turned into a well-formed
   * HTTP response by the throwing code (or by Spring itself for a request-shape problem like a
   * missing parameter) — routine enough that alerting on every one would be noise, not signal.</p>
   */
  @Override
  protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex,
      @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode,
      WebRequest request) {

    if (statusCode.is4xxClientError()) {
      dtlLogger.warn(ex.getMessage());
    } else {
      dtlLogger.error(ex);
    }

    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  /**
   * Handles HttpStatusException.
   * 
   * @param exception exception
   * @param request request
   * @return {@code ResponseEntity<?>}
   */
  @ExceptionHandler(HttpStatusException.class)
  public ResponseEntity<?> handleHttpStatusException(HttpStatusException exception,
      WebRequest request) {
    return ResponseEntity.status(exception.getHttpStatus())
        .body(new StatusResponse(exception.getHttpStatus().name()));
  }

  /**
   * Handles Throwable.
   * 
   * @param exception exception 
   * @return ErrorResponse
   */
  @ExceptionHandler(Throwable.class)
  public ErrorResponse handleThrowable(Throwable exception) {

    LogUtil.logSystemError(dtlLogger, exception);

    // app dependent procedures, like sending mail.
    if (actionOnThrowable != null) {
      Objects.requireNonNull(actionOnThrowable).execute(exception);
    }

    return ErrorResponse.create(exception, HttpStatusCode.valueOf(501), "Internal Server Error...");
  }
}
