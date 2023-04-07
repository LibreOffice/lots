/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Performs an asynchronous search in a {@link Datasource}.
 */
public class AsyncSearch
{

  private Map<String, String> searchQuery;
  private Datasource datasource;

  /**
   * A new search query.
   *
   * @param searchQuery
   *          The parameter of the search.
   * @param datasource
   *          The {@link Datasource}.
   */
  public AsyncSearch(Map<String, String> searchQuery, Datasource datasource)
  {
    this.searchQuery = searchQuery;
    this.datasource = datasource;
  }

  /**
   * Start an asynchronous search.
   *
   * @return A future with the results. Never null.
   */
  public CompletableFuture<QueryResults> runSearchAsync()
  {
    return CompletableFuture.supplyAsync(() -> {

      if (searchQuery == null || datasource == null)
      {
        return new QueryResultsList(Collections.emptyList());
      }

      return search(searchQuery);
    });
  }

  private QueryResults search(Map<String, String> query)
  {
    List<QueryPart> parts = new ArrayList<>();

    for (Map.Entry<String, String> entry : query.entrySet())
    {
      QueryPart qp = new QueryPart(entry.getKey(), entry.getValue());
      parts.add(qp);
    }

    return datasource.find(parts);
  }

}
