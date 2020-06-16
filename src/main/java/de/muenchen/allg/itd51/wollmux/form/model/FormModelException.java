package de.muenchen.allg.itd51.wollmux.form.model;

/**
 * An invalid form model.
 */
public class FormModelException extends Exception
{

  private static final long serialVersionUID = 5470806103796601018L;

  /**
   * An invalid form model.
   *
   * @param message
   *          The description of the error.
   * @param cause
   *          The causing exception.
   */
  public FormModelException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * An invalid form model.
   *
   * @param message
   *          The description of the error.
   */
  public FormModelException(String message)
  {
    super(message);
  }

}
