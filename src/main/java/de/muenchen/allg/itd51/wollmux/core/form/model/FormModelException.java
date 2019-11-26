package de.muenchen.allg.itd51.wollmux.core.form.model;

/**
 * Ein Fehler in der Konfiguration des Formulars.
 * 
 * @author daniel.sikeler
 *
 */
public class FormModelException extends Exception
{

  private static final long serialVersionUID = 5470806103796601018L;

  public FormModelException()
  {
    super();
  }

  public FormModelException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public FormModelException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public FormModelException(String message)
  {
    super(message);
  }

  public FormModelException(Throwable cause)
  {
    super(cause);
  }

}
