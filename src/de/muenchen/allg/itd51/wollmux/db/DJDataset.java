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

  
  
  /**
   * Schreibt newValue als neuen Wert des Datensatzes in Spalte columnName in den LOS des DJ.
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void set(String columnName, String newValue) throws ColumnNotFoundException;
  
  /**
   * Liefert true, falls die Spalte columnName dieses Datensatzes nicht aus
   * den Hintergrunddatenbank kommt, sondern aus dem lokalen Override-Speicher
   * des DJ.
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException;
  
  /**
   * Verwirft den Wert im LOS für Spalte columnName dieses Datensatzes und 
   * verknüpft die Spalte wieder mit der Hintergrunddatenbank.
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void discardLocalOverride(String columnName) throws ColumnNotFoundException;
  
  
}
