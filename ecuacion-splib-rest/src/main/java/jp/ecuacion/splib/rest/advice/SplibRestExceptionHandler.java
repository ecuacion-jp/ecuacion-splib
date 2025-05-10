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

import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.splib.rest.exception.HttpStatusException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
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
    return new ResponseEntity<>(new LinkedMultiValueMap<String, String>(),
        exception.getHttpStatus());
  }

  /**
   * Handles Throwable.
   * 
   * @param exception exception 
   * @return ErrorResponse
   */
  @ExceptionHandler(Throwable.class)
  public ErrorResponse handleThrowable(Throwable exception) {

    LogUtil.logSystemError(new DetailLogger(this), exception);

    return ErrorResponse.create(exception, HttpStatusCode.valueOf(501), "Internal Server Error...");
  }
}
