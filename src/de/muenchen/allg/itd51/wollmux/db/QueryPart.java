//TODO L.m()
/*
* Dateiname: QueryPart.java
* Projekt  : WollMux
* Funktion : Teil einer Datenbankabfrage
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 31.10.2005 | BNK | Erstellung
* 03.11.2005 | BNK | besser kommentiert
* 14.11.2005 | BNK | null Suchstring als leerer String interpretiert
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

/** Teil einer Datenbankabfrage. Zur Zeit einfach nur ein Spaltenname und
 * ein Suchstring. Selektiert werden alle Datensätze, die in der entsprechenden
 * Spalte den Suchstring haben. Der Suchstring kann
 * vorne und/oder hinten genau ein Sternchen '*' stehen haben, um
 * Präfix/Suffix/Teilstring-Suche zu realisieren. Folgen mehrerer Sternchen
 * oder Sternchen in der Mitte des Suchstrings sind verboten und
 * produzieren undefiniertes Verhalten. Ebenso verboten ist ein
 * Suchstring, der nur Sternchen enthält oder einer der leer ist.
 */
public class QueryPart
{
  private String columnName;
  private String searchString;
  
  public QueryPart(String spaltenName, String suchString)
  {
    if (spaltenName == null) throw new NullPointerException();
    if (suchString == null) 
      suchString = "";
    columnName = spaltenName;
    searchString = suchString;
  }
  
  /**
   * Liefert den Name der zu testenden Spalten.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getColumnName() {return columnName;}
  
  /**
   * Liefert den Suchstring auf den getestet werden soll. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getSearchString() {return searchString;} 
}
