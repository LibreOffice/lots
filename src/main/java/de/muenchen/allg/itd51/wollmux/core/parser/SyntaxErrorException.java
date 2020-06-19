package de.muenchen.allg.itd51.wollmux.core.parser;

/**
 * Signalisiert einen Fehler in einer zu parsenden Zeichenfolge
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SyntaxErrorException extends Exception
{
  /**
   * keine Ahnung was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = 7215084024054862356L;
  public SyntaxErrorException() {super();}
  public SyntaxErrorException(String message) {super(message);}
  public SyntaxErrorException(String message, Throwable cause) {super(message,cause);}
  public SyntaxErrorException(Throwable cause) {super(cause);}
}
