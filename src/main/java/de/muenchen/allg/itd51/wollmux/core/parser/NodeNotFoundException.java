package de.muenchen.allg.itd51.wollmux.core.parser;

/**
 * Signalisiert, dass ein gesuchter Knoten nicht gefunden wurde.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class NodeNotFoundException extends Exception
{
  private static final long serialVersionUID = 3441011738115879891L;
  public NodeNotFoundException() {super();}
  public NodeNotFoundException(String message) {super(message);}
  public NodeNotFoundException(String message, Throwable cause) {super(message,cause);}
  public NodeNotFoundException(Throwable cause) {super(cause);}
}
