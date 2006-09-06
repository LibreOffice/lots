/*
* Dateiname: InsertionModel.java
* Projekt  : WollMux
* Funktion : Stellt eine Einfügestelle im Dokument (insertValue oder insertFormValue) dar.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 06.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Stellt eine Einfügestelle im Dokument (insertValue oder insertFormValue) dar.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class InsertionModel
{
  /** 
   * Konstante für {@link #sourceType}, die angibt, dass die Daten für die Einfügung
   * aus einer externen Datenquelle kommen. 
   */
  private static final int DATABASE_TYPE = 0;
  
  /** 
   * Konstante für {@link #sourceType}, die angibt, dass die Daten für die Einfügung
   * aus dem Formular kommen. 
   */
  private static final int FORM_TYPE = 1;
  
  /**
   * Gibt an, um woher die Einfügung ihre Daten bezieht.
   * @see #FORM_TYPE
   * @see #DATABASE_TYPE
   */
  private int sourceType = FORM_TYPE;
  
  /**
   * Der Name des Bookmarks, das diese Einfügestelle umschließt.
   */
  private String bookmarkName;
  
  
}
