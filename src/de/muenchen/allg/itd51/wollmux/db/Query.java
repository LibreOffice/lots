/*
* Dateiname: Query.java
* Projekt  : WollMux
* Funktion : Speichert den Namen einer Datenquelle sowie eine Suchanfrage darauf.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 24.05.2006 | BNK | Erstellung
* 26.05.2006 | BNK | +getQueryParts()
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Speichert den Namen einer Datenquelle sowie eine Suchanfrage darauf.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Query implements Iterable<QueryPart>
{
  /**
   * Liste von QueryParts, die die eigentliche Query beschreiben.
   */
  private List<QueryPart> listOfQueryParts;
  
  /**
   * Der Name der Datenquelle auf der die Suche erfolgen soll.
   */
  private String dbName;
  
  /**
   * Erzeugt eine neue Query.
   * @param dbName der Name die Datenquelle auf der die Suche erfolgen soll.
   * @param listOfQueryParts Liste von QueryParts, die die eigentliche Query 
   *        beschreiben. ACHTUNG! Wird als Referenz eingebunden.
   */
  public Query(String dbName, List<QueryPart> listOfQueryParts)
  {
    this.listOfQueryParts = listOfQueryParts;
    this.dbName = dbName;
  }
  
  /**
   * Liefert den Namen der Datenquelle auf der die Suche erfolgen soll. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getDatasourceName() {return dbName;}
  
  /**
   * Iteriert über die QueryParts, die diese Suchanfrage ausmachen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Iterator<QueryPart> iterator() {return listOfQueryParts.iterator();}
  
  /**
   * Liefert die Liste der QueryParts die diese Suchanfrage definieren.
   */
  public List<QueryPart> getQueryParts() {return new Vector<QueryPart>(listOfQueryParts);}
  
  public int numberOfQueryParts() { return listOfQueryParts.size();}
}
