/*
* Dateiname: Dataset.java
* Projekt  : WollMux
* Funktion : Interface für Datensätze einer Tabelle.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 06.10.2005 | BNK | Erstellung
* 14.10.2005 | BNK | ->Interface
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

/**
 * TODO Doku
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Dataset
{
  public String get(String spaltenName);
}
