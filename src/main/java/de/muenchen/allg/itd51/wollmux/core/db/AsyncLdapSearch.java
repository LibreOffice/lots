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
