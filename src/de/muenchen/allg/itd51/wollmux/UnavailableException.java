//TODO L.m()
/*
* Dateiname: UnavailableException.java
* Projekt  : WollMux
* Funktion : Zeigt an, dass die gewünschte(n) Funktion(en)/Daten nicht verfügbar sind.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 18.10.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

/**
 * Zeigt an, dass die gewünschte(n) Funktion(en)/Daten nicht verfügbar sind.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UnavailableException extends Exception
{

  /**
   * Unterdrückt Warnung.
   */
  private static final long serialVersionUID = 5874615503838299278L;

  public UnavailableException()
  {
    super();
  }

  public UnavailableException(String message)
  {
    super(message);
  }

  public UnavailableException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public UnavailableException(Throwable cause)
  {
    super(cause);
  }

}
