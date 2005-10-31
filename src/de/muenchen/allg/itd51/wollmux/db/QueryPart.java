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
    columnName = spaltenName;
    searchString = suchString;
  }
  
  public String getColumnName() {return columnName;}
  public String getSearchString() {return searchString;} 
}
