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
* 24.10.2005 | BNK | +copy()
*                  | +remove()
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
   * Schreibt newValue als neuen Wert des Datensatzes in Spalte columnName 
   * in den LOS des DJ, jedoch nur falls der Datensatz bereits aus dem LOS
   * kommt (also {@link #isFromLOS()} true liefert).
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert. 
   * @throws UnsupportedOperationException, falls dieser Datensatz nicht aus
   *         dem LOS kommt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void set(String columnName, String newValue) throws ColumnNotFoundException, UnsupportedOperationException;
  
  /**
   * Liefert true, falls die Spalte columnName dieses Datensatzes nicht aus
   * den Hintergrunddatenbank kommt, sondern aus dem lokalen Override-Speicher
   * des DJ.
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException;
  
  /**
   * Liefert true, falls dieser Datensatz aus dem Lokalen Override Speicher
   * kommt. ACHTUNG! Dies bedeutet nicht, dass es eine Spalte gibt, für die
   * hasLocalOverride(Spalte) true liefert, da der LOS auch Datensätze erlaubt,
   * bei denen alle Spalten noch mit der Hintergrunddatenbank verknüpft sind.
   * Zum Beispiel wird ein Datensatz nicht automatisch aus dem LOS entfernt,
   * wenn für alle Spalten discardLocalOverride() aufgerufen wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isFromLOS();
  
  /**
   * Verwirft den Wert im LOS für Spalte columnName dieses Datensatzes und 
   * verknüpft die Spalte wieder mit der Hintergrunddatenbank.
   * ACHTUNG! Ein Datensatz bei dem der lokale Override für alle Spalten
   * discardet wurde wird NICHT automatisch aus dem LOS entfernt. Insbesondere
   * liefert isFromLOS() weiterhin true.
   * Diese Funktion kann auch ohne Fehler oder Exception aufgerufen werden, 
   * falls der Datensatz keinen lokal Override für die Spalte hat oder sogar
   * gar nicht aus dem LOS kommt. Die Spalte muss auf jeden Fall existieren.
   * @throws ColumnNotFoundException falls keine Spalte namens columnName existiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void discardLocalOverride(String columnName) throws ColumnNotFoundException;
  
  /**
   * Legt eine Kopie dieses Datensatzes im LOS an. Achtung! Dies verändert
   * nicht den Rückgabewert von {@link #isFromLOS()}, da dieser Datensatz
   * selbst dadurch nicht verändert wird.
   * 
   * @return die neue Kopie.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset copy();
  
  /**
   * Entfernt diesen Datensatz aus dem LOS. Achtung! Nach dieser Operation ist
   * der Datensatz ungültig. Insbesondere ist der Wert von {@link #isFromLOS()}
   * nicht definiert.
   * @throws UnsupportedOperationException falls dieser Datensatz nicht aus
   * dem LOS kommt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void remove() throws UnsupportedOperationException;
}
