package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * Wird geworfen beim Versuch, auf eine Spalte zuzugreifen, die
 * nicht existiert.
 *  
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class NoBackingStoreException extends Exception
{
  private static final long serialVersionUID = -1672676873427003242L;
  public NoBackingStoreException() {super();}
  public NoBackingStoreException(String message) {super(message);}
  public NoBackingStoreException(String message, Throwable cause) {super(message,cause);}
  public NoBackingStoreException(Throwable cause) {super(cause);}
}
