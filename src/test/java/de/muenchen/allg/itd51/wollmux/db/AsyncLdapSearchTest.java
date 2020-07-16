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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.db.mock.MockDatasource;

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
