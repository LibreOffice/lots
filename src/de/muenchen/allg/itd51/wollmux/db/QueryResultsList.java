/*
* Dateiname: QueryResultsList.java
* Projekt  : WollMux
* Funktion : Eine einfache Klasse um eine Liste als QueryResults zur
*            Verfügung zu stellen.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 28.10.2005 | BNK | Erstellung
* 03.11.2005 | BNK | besser kommentiert
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
 * Eine einfache Klasse um eine Liste als QueryResults zur
 *  Verfügung zu stellen.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
class QueryResultsList implements QueryResults
{
  /**
   * Die Liste der Datasets.
   */
  private List data;
  
  /**
   * Erzeugt eine neue QueryResultsList, die die Elemente enthält 
   * die iter zurückliefert (müssen Datasets sein!).
   * @param count dient der Optimierung und sollte die Anzahl der Elemente
   * enthalten, die der Iterator zurückliefern wird. Ist dies nicht bekannt,
   * kann 0 übergeben werden.
   */
  public QueryResultsList(Iterator iter, int count)
  {
    data = new Vector(count);
    while(iter.hasNext()) data.add(iter.next());
  }

  /**
   * Erzeugt eine neue QueryResultsList aus einer bestehenden Liste.
   * Achtung! Die Liste wird nicht kopiert, sondern als Referenz übernommen.
   */
  public QueryResultsList(List datasets)
  {
    data = datasets;
  }
  
  public int size() { return data.size();}
  
  public Iterator iterator(){ return data.iterator(); }
  
  public boolean isEmpty() { return data.isEmpty(); }
}
