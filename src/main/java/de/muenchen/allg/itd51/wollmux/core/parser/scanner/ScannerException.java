package de.muenchen.allg.itd51.wollmux.core.parser.scanner;

import java.io.IOException;

/**
 * Exception class for the scanner module.
 * 
 * @author Daniel Sikeler
 */
public class ScannerException extends IOException
{

  /** Serial Version ID. */
  private static final long serialVersionUID = 6441716168137433335L;

  /**
   * Constructor for scanner exceptions.
   * 
   * @param message
   *          Description of the error.
   */
  public ScannerException(final String message)
  {
    super(message);
  }

  /**
   * Constructor for scanner exceptions.
   * 
   * @param cause
   *          The error, which preceeds this exception.
   */
  public ScannerException(final Throwable cause)
  {
    super(cause);
  }

  /**
   * Constructor for scanner exceptions.
   * 
   * @param message
   *          Description of the error.
   * @param cause
   *          An other exception, which caused this one.
   */
  public ScannerException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

}
