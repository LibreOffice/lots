package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * Eine Datenquelle, die keine Datensätze enthält.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class EmptyDatasource implements Datasource
{
  private static QueryResults emptyResults =
    new QueryResultsList(new Vector<Dataset>(0));

  private List<String> schema;

  private String name;

  public EmptyDatasource(List<String> schema, String name)
  {
    this.schema = schema;
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection,
   *      long)
   */
  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    return emptyResults;
  }

  @Override
  public QueryResults getContents()
  {
    return emptyResults;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    return emptyResults;
  }
}
