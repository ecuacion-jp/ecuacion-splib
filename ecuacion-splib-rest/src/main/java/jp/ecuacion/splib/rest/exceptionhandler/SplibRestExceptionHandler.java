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
package jp.ecuacion.splib.rest.exceptionhandler;

import java.util.List;
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.lib.core.util.LogUtil;
import jp.ecuacion.splib.core.exceptionhandler.SplibRestExceptionHandlerAction;
import jp.ecuacion.splib.rest.dto.ViolationsResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Handles the three kinds of exceptions a splib-rest endpoint can raise, each with a
 * deliberately different treatment because each has a different <em>audience</em>.
 *
 * <ul>
 *   <li>{@link ViolationException} — for a business/validation failure whose message is meant
 *       to reach an actual human end user, e.g. a local/desktop app that calls this API on a
 *       user's behalf and displays the failure to them. Handled by
 *       {@link #handleViolationException}: the message(s) are localized to the request's
 *       locale, every violation {@code exception} carries is included (not just the first), and
 *       the status is always {@code 400 Bad Request} — no variety of statuses is needed because
 *       nothing on the calling side is expected to branch on it, only display the text.</li>
 *   <li>{@link org.springframework.web.server.ResponseStatusException} (and anything else
 *       {@link ResponseEntityExceptionHandler}'s built-in handling deals with) — for a failure
 *       whose message is meant for the developer/system on the other end of the API call (e.g.
 *       a server calling this API programmatically), not a human end user. Handled by
 *       {@link #handleExceptionInternal}: the message is used as-is, not localized, and the
 *       throwing code picks whichever status (any {@code 4xx}/{@code 5xx}) fits — callers are
 *       expected to branch on it.</li>
 *   <li>Anything else — a genuinely unanticipated exception (a bug, not a reported failure).
 *       Handled by {@link #handleThrowable}: logged as a system error, optionally alerted on via
 *       {@link #actionOnThrowable}, and answered with a generic {@code 501} — there is no
 *       meaningful message to return either a human or a developer in this case.</li>
 * </ul>
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
   * Handles {@link ViolationException} — see the class Javadoc for why this gets different
   * treatment (localized, all violations included, fixed status) from
   * {@link org.springframework.web.server.ResponseStatusException}.
   *
   * <p>Logged at {@code WARN}, like any other {@code 4xx}: a violation reports bad input, not a
   * server-side fault, so it gets neither the {@code "=== system error occurred ==="} marker nor
   * a stack trace (see {@link #handleExceptionInternal}) nor {@link #actionOnThrowable}.</p>
   *
   * @param exception the violation exception
   * @param request request, used only to resolve the response locale
   * @return {@code 400 Bad Request} with every violation's message, localized
   */
  @ExceptionHandler(ViolationException.class)
  public ResponseEntity<@NonNull ViolationsResponse> handleViolationException(
      ViolationException exception, WebRequest request) {

    @NonNull List<@NonNull String> messages =
        ExceptionUtil.getMessageList(exception.getViolations(), request.getLocale(), true);
    dtlLogger.warn(String.join(" / ", messages));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ViolationsResponse(messages));
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
   *
   * <p>For the same reason, this logs the message only — never via {@code DetailLogger}'s
   * {@code error(Throwable)}/{@code warn(Throwable)} overloads, which stamp a
   * {@code "=== system error occurred ==="} marker and a full stack trace onto the log.
   * A non-4xx status here is not necessarily an actual system fault either — e.g. a
   * {@code ResponseStatusException(INTERNAL_SERVER_ERROR, "scriptFilePath '...' not found")} is
   * an anticipated, cleanly-handled report of an operator's config mistake, and mislabeling it
   * as a "system error" would send operators chasing a code bug that isn't there. That marker
   * stays reserved for {@link #handleThrowable}, where it's actually true.</p>
   */
  @Override
  protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex,
      @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

    dtlLogger.info("SplibRestExceptionHandler#handleExceptionInternal called. "
        + "statusCode = " + statusCode);

    if (statusCode.is4xxClientError()) {
      dtlLogger.warn(ex.getMessage());
    } else {
      dtlLogger.error(ex.getMessage());
    }

    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  /**
   * Handles Throwable.
   * 
   * @param exception exception 
   * @return ErrorResponse
   */
  @ExceptionHandler(Throwable.class)
  public ErrorResponse handleThrowable(Throwable exception) {

    dtlLogger.info("SplibRestExceptionHandler#handleThrowable called.");
    LogUtil.logSystemError(dtlLogger, exception);

    // app dependent procedures, like sending mail.
    if (actionOnThrowable != null) {
      Objects.requireNonNull(actionOnThrowable).execute(exception);
    }

    return ErrorResponse.create(exception, HttpStatusCode.valueOf(501), "Internal Server Error...");
  }
}
