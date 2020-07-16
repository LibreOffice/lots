/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.db;

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
