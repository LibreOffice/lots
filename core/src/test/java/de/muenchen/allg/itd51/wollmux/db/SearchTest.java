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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDatasource;

public class SearchTest
{

  @Test
  public void testSearchWithStrategy() throws Exception
  {
    SearchStrategy strategy = SearchStrategy
        .parse(new ConfigThingy("", "SearchStrategy(test (column \"${suchanfrage1}\"))"));
    Map<String, Datasource> datasources = Map.of("test", new MockDatasource());
    assertNull(Search.search(null, null, null));
    assertNull(Search.search("value", null, null));
    assertNull(Search.search("value", strategy, null));
    QueryResults results = Search.search("value", strategy, datasources);
    assertEquals(1, results.size());
    results = Search.search(".", strategy, datasources);
    assertEquals(0, results.size());
    results = Search.search("*", strategy, datasources);
    assertEquals(0, results.size());
    results = Search.search("value*", strategy, datasources);
    assertEquals(0, results.size());
    results = Search.search("", strategy, datasources);
    assertEquals(0, results.size());

    strategy = SearchStrategy.parse(new ConfigThingy("", "SearchStrategy(test ())"));
    results = Search.search("value", strategy, datasources);
    assertEquals(2, results.size());

    strategy = SearchStrategy.parse(new ConfigThingy("", "SearchStrategy(test ())"));
    results = Search.search("value", strategy, datasources);
    assertEquals(2, results.size());

    strategy = SearchStrategy.parse(
        new ConfigThingy("", "SearchStrategy(test (column \"${suchanfrage1}\") test (column \"${suchanfrage1}3\"))"));
    datasources = Map.of("test", new MockDatasource("test", List.of("column", "column2"), List
        .of(
        new MockDataset("ds", Map.of("column", "value", "column2", "value2")),
        new MockDataset("ds2", Map.of("column", "value3", "column2", "value4")),
            new MockDataset("ds2", Map.of("column", "value3", "column2", "value4")))));
    results = Search.search("value", strategy, datasources);
    assertEquals(2, results.size());
  }
}
