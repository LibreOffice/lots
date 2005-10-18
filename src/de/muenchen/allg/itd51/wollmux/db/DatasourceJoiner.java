/*
* Dateiname: DatasourceJoiner.java
* Projekt  : WollMux
* Funktion : stellt eine virtuelle Datenbank zur Verfügung, die ihre Daten
*            aus verschiedenen Hintergrunddatenbanken zieht.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 06.10.2005 | BNK | Erstellung
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
public class DatasourceJoiner
{
  //Suche nach "Vorname Name"
  //           "Vorn Nam"
  //           "Nam"
  //           "Nam Vorn"
  //           "Nam, Vorn"
  //           "vorname.nachname"
  //           "vorname.nachname@muenchen.de"
  //           "ITD5.1"
  //           "ITD 5.1"
  //           "ITD-5.1"
  //           "D-III-ITD-5.1"
  //           "D-HAIII-ITD-5.1"
  //           "D-HAIII-ITD5.1"
  //           "D-HAIII-ITD 5.1"
  //           "D"
  //           "ITD5"
  //           "D-HAIII"
  //           "5.1"
  
  //suchString kann vorne und/oder hinten ein % haben zur prefix/suffix/teilstring-suche
  public QueryResults find(String spaltenName, String suchString)
  {
    return null;
  }
  
  public QueryResults find(String spaltenName1, String suchString1,String spaltenName2, String suchString2)
  {
    return null;
  }
  
  public QueryResults find(String spaltenName1, String suchString1,String spaltenName2, String suchString2,String spaltenName3, String suchString3)
  {
    return null;
  }
  
  /**
   * Liefert den momentan in der Datensatzliste ausgewählten Datensatz.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DJDataset getSelectedDataset() throws DatasetNotFoundException
  {
    return new TestDJDataset();
  }
  
//TODO LOS = Local Override Storage  
  
  
}
