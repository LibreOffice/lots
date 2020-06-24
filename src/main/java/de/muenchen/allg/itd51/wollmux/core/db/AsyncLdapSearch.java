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
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Performs an asynchronous search through DatasourceJoiner. Depending on the underlying active main
 * datasource, the query searches against ldap, configthingy, ooodatasource and more datasources.
 */
public class AsyncLdapSearch
{
  private Map<String, String> searchQuery = new HashMap<>();
  private DatasourceJoiner dj = null;

  public AsyncLdapSearch(Map<String, String> searchQuery, DatasourceJoiner dj)
  {
    this.searchQuery = searchQuery;
    this.dj = dj;
  }

  public CompletableFuture<QueryResults> runLdapSearchAsync()
  {
    return this.asyncLdapSearch;
  }

  private CompletableFuture<QueryResults> asyncLdapSearch = CompletableFuture.supplyAsync(() -> {

    if (searchQuery == null || dj == null)
      return null;

    return Search.search(searchQuery, dj);
  });

}
