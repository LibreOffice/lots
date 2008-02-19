//TODO L.m()
/*
* Dateiname: ConfigurationErrorException.java
* Projekt  : WollMux
* Funktion : wird geworfen, wenn eine Fehlkonfiguration festgestellt wird (d.h. Benutzer hat Config verbockt)
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 11.10.2005 | BNK | Erstellung
* 13.10.2005 | BNK | +serialVersionUID
* 14.10.2005 | BNK | keine RuntimeException mehr
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

/**
 * wird geworfen, wenn eine Fehlkonfiguration festgestellt wird (d.h. Benutzer hat Config verbockt)
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ConfigurationErrorException extends Exception
{
  /**
   * keine Ahnung, was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = -2457549809413613658L;
  public ConfigurationErrorException() {};
  public ConfigurationErrorException(String message) {super(message);}
  public ConfigurationErrorException(String message, Throwable cause) {super(message,cause);}
  public ConfigurationErrorException(Throwable cause) {super(cause);}
}
