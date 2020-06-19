package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Ein Container für Ergebnisse einer Datenbankafrage zusammen mit dem zugehörigen
 * Schema.
 */
public class QueryResultsWithSchema implements QueryResults
{
  protected QueryResults results;

  protected List<String> schema;

  /**
   * Constructs an empty QueryResultsWithSchema with empty schema.
   */
  public QueryResultsWithSchema()
  {
    results = new QueryResultsList(new ArrayList<Dataset>());
    schema = new ArrayList<>();
  }

  /**
   * Erzeugt ein neues QueryResultsWithSchema, das den Inhalt von res und das Schema
   * schema zusammenfasst. ACHTUNG! res und schema werden als Referenzen übernommen.
   */
  public QueryResultsWithSchema(QueryResults res, List<String> schema)
  {
    this.schema = schema;
    this.results = res;
  }

  @Override
  public int size()
  {
    return results.size();
  }

  @Override
  public Iterator<Dataset> iterator()
  {
    return results.iterator();
  }

  @Override
  public boolean isEmpty()
  {
    return results.isEmpty();
  }

  public Set<String> getSchema()
  {
    return new HashSet<>(schema);
  }

}
