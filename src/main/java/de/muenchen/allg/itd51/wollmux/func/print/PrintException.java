package de.muenchen.allg.itd51.wollmux.func.print;

/**
 * Exception for indicating any error during printing.
 */
public class PrintException extends Exception
{

  private static final long serialVersionUID = 8374980631449034838L;

  /**
   * Create a new exception with message and cause.
   *
   * @param message
   *          A human readable exception description.
   * @param cause
   *          The cause. (A null value is permitted, and indicates that the cause is nonexistent or
   *          unknown.
   */
  public PrintException(String message, Throwable cause)
  {
    super(message, cause);
  }

}
