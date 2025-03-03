/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.db;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the union of multiple QueryResults.
 *
 * Duplicate results are filtered out. It is a mathematical union.
 * @author daniel.sikeler
 */
public class QueryResultsSet implements QueryResults
{
  /**
   * Mathematical union of QueryResults.
   */
  private final List<Dataset> results = new ArrayList<>();
  /**
   * This comparator is used to recognize the equality of two datasets.
   */
  private final Comparator<Dataset> comparator;

  /**
   * Creates an empty set.
   * @param comparator The comparator to use for comparisons when adding.
   */
  public QueryResultsSet(Comparator<Dataset> comparator)
  {
    this.comparator = comparator;
  }

  /**
   * Creates a results list with the records from queryResults.
   * @param comparator The comparator to use for comparisons when adding.
   * @param queryResults The list of new records.
   */
  public QueryResultsSet(Comparator<Dataset> comparator, QueryResults queryResults)
  {
    this(comparator);
    addAll(queryResults);
  }

  /**
   * Adds a new record as long as it is not already included.
   * @param dataset The new record.
   */
  public void add(Dataset dataset)
  {
    boolean present = false;
    for (Dataset ds : results)
    {
      if (comparator.compare(dataset, ds) == 0)
      {
        present = true;
        break;
      }
    }
    if (!present)
    {
      results.add(dataset);
    }
  }

  /**
   * Adds all records to the list as long as they are not already included.
   * @param queryResults List of new records.
   */
  public void addAll(QueryResults queryResults)
  {
    for (Dataset ds : queryResults)
    {
      add(ds);
    }
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

}
