/*
* Dateiname: TimeoutException.java
* Projekt  : WollMux
* Funktion : wird geworfen, wenn etwas nicht in einem gegebenen Zeitrahmen getan werden konnte.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 28.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

/**
 * wird geworfen, wenn etwas nicht in einem gegebenen Zeitrahmen getan werden konnte.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TimeoutException extends Exception
{
  private static final long serialVersionUID = -2334582583309727084L;
  public TimeoutException() {};
  public TimeoutException(String message) {super(message);}
  public TimeoutException(String message, Throwable cause) {super(message,cause);}
  public TimeoutException(Throwable cause) {super(cause);}
}
