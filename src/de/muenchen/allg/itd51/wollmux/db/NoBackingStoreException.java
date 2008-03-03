/*
* Dateiname: NoBackingStoreException.java
* Projekt  : WollMux
* Funktion : Wird geworfen beim Versuch, auf eine Spalte zuzugreifen, die
* nicht existiert.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 14.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

/**
 * Wird geworfen beim Versuch, auf eine Spalte zuzugreifen, die
 * nicht existiert.
 *  
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class NoBackingStoreException extends Exception
{
  private static final long serialVersionUID = -1672676873427003242L;
  public NoBackingStoreException() {};
  public NoBackingStoreException(String message) {super(message);}
  public NoBackingStoreException(String message, Throwable cause) {super(message,cause);}
  public NoBackingStoreException(Throwable cause) {super(cause);}
}
