package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * Wird geworfen, falls kein passender Datensatz gefunden wurde und
 * die Möglichkeit, eine leere Ergebnisliste zurückzugeben nicht
 * existiert. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasetNotFoundException extends Exception
{
  private static final long serialVersionUID = -2369645834768269537L;
  public DatasetNotFoundException() {super();}
  public DatasetNotFoundException(String message) {super(message);}
  public DatasetNotFoundException(String message, Throwable cause) {super(message,cause);}
  public DatasetNotFoundException(Throwable cause) {super(cause);}
}
