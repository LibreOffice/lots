/*
* Dateiname: ColumnNotFoundException.java
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
public class ColumnNotFoundException extends Exception
{
  private static final long serialVersionUID = -5096388185337055277L;
  public ColumnNotFoundException() {};
  public ColumnNotFoundException(String message) {super(message);}
  public ColumnNotFoundException(String message, Throwable cause) {super(message,cause);}
  public ColumnNotFoundException(Throwable cause) {super(cause);}
}
