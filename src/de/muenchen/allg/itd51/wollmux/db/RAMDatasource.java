/*
* Dateiname: RAMDatasource.java
* Projekt  : WollMux
* Funktion : Oberklasse für Datasources, die ihre Daten vollständig
*            im Speicher halten
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 31.10.2005 | BNK | Erstellung
* 03.11.2005 | BNK | besser kommentiert
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.TimeoutException;
import de.muenchen.allg.itd51.wollmux.db.checker.ColumnContainsChecker;
import de.muenchen.allg.itd51.wollmux.db.checker.ColumnIdentityChecker;
import de.muenchen.allg.itd51.wollmux.db.checker.ColumnPrefixChecker;
import de.muenchen.allg.itd51.wollmux.db.checker.ColumnSuffixChecker;
import de.muenchen.allg.itd51.wollmux.db.checker.DatasetChecker;
import de.muenchen.allg.itd51.wollmux.db.checker.MatchAllDatasetChecker;

/**
 * Oberklasse für Datasources, die ihre Daten vollständig
 *  im Speicher halten
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class RAMDatasource implements Datasource
{
  /**
   * Das Schema dieser Datenquelle.
   */
  private Set schema;
  
  /**
   * Liste aller Datasets, die in dieser Datasource gespeichert sind.
   */
  private List data;
  
  /**
   * Der Name dieser Datenquelle.
   */
  private String name;
  
  /**
   * Erzeugt eine neue RAMDatasource mit Namen name.
   * data und schema werden direkt als Referenz eingebunden, nicht kopiert.
   * @param name der Name der Datenquelle
   * @param schema das Schema der Datenquelle
   * @param data die Datensätze der Datenquelle
   */
  public RAMDatasource(String name, Set schema, List data)
  {
    init(name, schema, data);
  }
  
  /**
   * Führt die Initialisierungsaktionen des Konstruktors mit den gleichen
   * Parametern aus. Diese Methode sollte von abgeleiteten Klassen verwendet
   * werden, wenn sie den Konstruktor ohne Argumente verwenden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected void init(String name, Set schema, List data)
  {
    this.schema = schema; 
    this.data = data;
    this.name = name;
  }
  
  /**
  * Erzeugt eine uninitialisierte RAMDatasource. Eine abgeleitete Klasse, die diesen
  * Konstruktor verwendet sollte init() aufrufen, um die nötigen Initialisierungen
  * zu erledigen. 
  */
  protected RAMDatasource(){};

  public Set getSchema()
  { //TESTED
    return new HashSet(schema);
  }

  public QueryResults getDatasetsByKey(Collection keys, long timeout)
      throws TimeoutException
  { //TESTED
    Vector res = new Vector();
    Iterator iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      if (keys.contains(ds.getKey())) res.add(ds);
    }
        
    return new QueryResultsList(res);
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */
  public QueryResults find(List query, long timeout) throws TimeoutException
  { //TESTED
    DatasetChecker checker = new MatchAllDatasetChecker();
    Iterator iter = query.iterator();
    while (iter.hasNext())
    {
      QueryPart part = (QueryPart)iter.next();
      checker = checker.and(DatasetChecker.makeChecker(part.getColumnName(), part.getSearchString()));
    }
    
    List results = new Vector();
    
    iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      if (checker.matches(ds)) results.add(ds);
    }
    return new QueryResultsList(results);
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  public String getName()
  {
    return name;
  }

  
  
  
}
