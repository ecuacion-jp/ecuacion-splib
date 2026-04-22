package jp.ecuacion.splib.web.exceptionhandler;

import jp.ecuacion.lib.core.exception.ViolationWarningException;
import jp.ecuacion.lib.core.violation.Violations;

/**
 * Throw warning exception.
 */
public class ViolationWebWarningException extends ViolationWarningException {

  private static final long serialVersionUID = 1L;
  
  /**
   * The buttonId javascript presses when a user pressed okay on warning popup window.
   */
  private String buttonIdPressedIfConfirmed;

  /**
   * Constructs a new instance with the {@code Violations} that caused this exception.
   *
   * @param violations the violations that triggered the throw
   */
  public ViolationWebWarningException(Violations violations, String buttonIdPressedIfConfirmed) {
    super(violations);
    this.buttonIdPressedIfConfirmed = buttonIdPressedIfConfirmed;
  }

  public String getButtonIdPressedIfConfirmed() {
    return buttonIdPressedIfConfirmed;
  }
}
