package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Speichert den Namen einer Datenquelle sowie eine Suchanfrage darauf.
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
   */
  public String getDatasourceName() {return dbName;}
  
  /**
   * Iteriert über die QueryParts, die diese Suchanfrage ausmachen.
   */
  @Override
  public Iterator<QueryPart> iterator() {return listOfQueryParts.iterator();}
  
  /**
   * Liefert die Liste der QueryParts die diese Suchanfrage definieren.
   */
  public List<QueryPart> getQueryParts() {return new Vector<>(listOfQueryParts);}
  
  public int numberOfQueryParts() { return listOfQueryParts.size();}
}
