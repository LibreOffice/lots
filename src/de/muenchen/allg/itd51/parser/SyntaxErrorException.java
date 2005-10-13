/*
* Dateiname: SyntaxErrorException.java
* Projekt  : n/a
* Funktion : Signalisiert einen Fehler in einer zu parsenden Zeichenfolge 
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 06.10.2005 | BNK | Erstellung
* 11.10.2005 | BNK | Doku
* 13.10.2005 | BNK | +serialVersionUID
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.parser;

/**
 * Signalisiert einen Fehler in einer zu parsenden Zeichenfolge
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SyntaxErrorException extends RuntimeException
{
  /**
   * keine Ahnung was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = 7215084024054862356L;
  public SyntaxErrorException() {};
  public SyntaxErrorException(String message) {super(message);}
  public SyntaxErrorException(String message, Throwable cause) {super(message,cause);}
  public SyntaxErrorException(Throwable cause) {super(cause);}
}
