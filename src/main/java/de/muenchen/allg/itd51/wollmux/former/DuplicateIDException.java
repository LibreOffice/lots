package de.muenchen.allg.itd51.wollmux.former;

/**
 * Wird geworfen, wenn versucht wird, die ID eines Elementes auf einen bereits von
 * einem anderen Element verwendeten Wert zu ändern.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DuplicateIDException extends Exception
{
  /**
   * keine Ahnung, was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = 4349143792168156649L;
  public DuplicateIDException() {};
  public DuplicateIDException(String message) {super(message);}
  public DuplicateIDException(String message, Throwable cause) {super(message,cause);}
  public DuplicateIDException(Throwable cause) {super(cause);}
}
