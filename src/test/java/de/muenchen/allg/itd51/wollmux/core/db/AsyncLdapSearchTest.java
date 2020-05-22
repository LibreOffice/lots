package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDatasource;

class AsyncLdapSearchTest
{

  @Test
  void testAsyncLdapSearch() throws Exception
  {
    DatasourceJoiner dsJoiner = new DatasourceJoiner(Map.of("test", new MockDatasource()), "test",
        new LocalOverrideStorageDummyImpl());
    AsyncLdapSearch search = new AsyncLdapSearch(Map.of("column", "value"), dsJoiner);
    CompletableFuture<QueryResults> completable = search.runLdapSearchAsync();
    assertEquals(1, completable.get().size());

    search = new AsyncLdapSearch(Map.of("column", "value"), null);
    assertNull(search.runLdapSearchAsync().get());

    search = new AsyncLdapSearch(null, dsJoiner);
    assertNull(search.runLdapSearchAsync().get());
  }

}
