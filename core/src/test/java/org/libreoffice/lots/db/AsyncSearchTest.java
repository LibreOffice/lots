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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.db.AsyncSearch;
import org.libreoffice.lots.db.QueryResults;
import org.libreoffice.lots.db.mock.MockDatasource;

public class AsyncSearchTest
{

  @Test
  public void testAsyncSearch() throws Exception
  {
    AsyncSearch search = new AsyncSearch(Map.of("column", "value"), new MockDatasource());
    CompletableFuture<QueryResults> completable = search.runSearchAsync();
    assertEquals(1, completable.get().size());

    search = new AsyncSearch(Map.of("column", "value"), null);
    assertTrue(search.runSearchAsync().get().isEmpty());

    search = new AsyncSearch(null, new MockDatasource());
    assertTrue(search.runSearchAsync().get().isEmpty());
  }

}
