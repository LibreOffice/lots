/*
* Dateiname: DJDataset.java
* Projekt  : WollMux
* Funktion : Ein vom DJ gelieferter Datensatz, der zu den Methoden von
*            Dataset noch DJ-spezifische Methoden anbietet.
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
 * TODO Doku
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface DJDataset extends Dataset
{

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Dataset#get(java.lang.String)
   */
  public String get(String spaltenName);
  
  /**
   * Liefert true, falls die Spalte columnName dieses Datensatzes nicht aus
   * den Hintergrunddatenbank kommt, sondern aus dem lokalen Override-Speicher
   * des DJ.
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException;
  
}
