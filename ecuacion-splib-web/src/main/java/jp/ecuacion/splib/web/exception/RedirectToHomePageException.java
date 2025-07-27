package jp.ecuacion.splib.web.exception;

import jp.ecuacion.lib.core.util.PropertyFileUtil;
import org.slf4j.event.Level;

/**
 * Transitions to home page.
 */
public class RedirectToHomePageException extends RedirectException {

  private static final long serialVersionUID = 1L;
  private static final String HOME_PAGE_PATH =
      PropertyFileUtil.getApplication("jp.ecuacion.splib.web.home-page");

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException() {
    this((Level) null, (String) null, (String) null);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(String messageId, String... messageArgs) {
    this((Level) null, (String) null, messageId, messageArgs);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(Level logLevel, String logString) {
    this(logLevel, logString, (String) null);
  }

  /**
   * Constructs a new instance.
   */
  public RedirectToHomePageException(Level logLevel, String logString, String messageId,
      String... messageArgs) {
    super(properHomePagePath(), logLevel, logString, messageId, messageArgs);
  }

  /**
   * See {@link RedirectException}.
   */
  public RedirectToHomePageException(Level logLevel, String messageId, String[] messageArgs) {
    super(properHomePagePath(), logLevel, messageId, messageArgs);
  }

  private static String properHomePagePath() {
    return (HOME_PAGE_PATH.startsWith("/") ? "" : "/") + HOME_PAGE_PATH;
  }
}
