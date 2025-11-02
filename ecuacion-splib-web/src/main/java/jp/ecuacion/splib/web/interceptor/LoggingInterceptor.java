package jp.ecuacion.splib.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Offers logging features.
 */
public class LoggingInterceptor implements HandlerInterceptor {

  private final DetailLogger detailLog = new DetailLogger(this);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    detailLog.info("session ID: " + request.getSession().getId() + ", thread ID: "
        + Thread.currentThread().threadId() + " : request process started. request: "
        + request.getRequestURI());

    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      @Nullable ModelAndView modelAndView) throws Exception {
    detailLog.info("session ID: " + request.getSession().getId() + ", thread ID: "
        + Thread.currentThread().threadId() + " : request process finished.");
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) throws Exception {
    detailLog.info("session ID: " + request.getSession().getId() + ", thread ID: "
        + Thread.currentThread().threadId() + " : view rendering finished.");
  }
}
