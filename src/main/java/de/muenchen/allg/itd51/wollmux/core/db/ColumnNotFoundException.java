package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * Wird geworfen beim Versuch, auf eine Spalte zuzugreifen, die
 * nicht existiert.
 *  
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ColumnNotFoundException extends Exception
{
  private static final long serialVersionUID = -5096388185337055277L;
  public ColumnNotFoundException() {super();}
  public ColumnNotFoundException(String message) {super(message);}
  public ColumnNotFoundException(String message, Throwable cause) {super(message,cause);}
  public ColumnNotFoundException(Throwable cause) {super(cause);}
}
