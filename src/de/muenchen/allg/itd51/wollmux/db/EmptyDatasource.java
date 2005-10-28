/*
* Dateiname: EmptyDatasource.java
* Projekt  : WollMux
* Funktion : Eine Datenquelle, die keine Datensätze enthält.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 28.10.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Collection;
import java.util.Set;
import java.util.Vector;

/**
 * Eine Datenquelle, die keine Datensätze enthält.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class EmptyDatasource implements Datasource
{
  private static QueryResults emptyResults = new QueryResultsList(new Vector(0));
  private Set schema;
  
  public EmptyDatasource(Set schema)  {this.schema = schema;}

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  public Set getSchema()
  {
    return schema;
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection, long)
   */
  public QueryResults getDatasetsByKey(Collection keys, long timeout)
  {
    return emptyResults;
  }
}
