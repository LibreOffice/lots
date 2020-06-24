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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDatasource;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class SearchTest
{

  @Test
  void testSearchWithStrategy() throws Exception
  {
    SearchStrategy strategy = SearchStrategy
        .parse(new ConfigThingy("", "Suchstrategie(test (column \"${suchanfrage1}\"))"));
    DatasourceJoiner dsJoiner = new DatasourceJoiner(Map.of("test", new MockDatasource()), "test",
        new LocalOverrideStorageDummyImpl());
    assertNull(Search.search(null, null, null, false));
    assertNull(Search.search("value", null, null, false));
    assertNull(Search.search("value", strategy, null, false));
    QueryResults results = Search.search("value", strategy, dsJoiner, false);
    assertEquals(1, results.size());
    results = Search.search(".", strategy, dsJoiner, true);
    assertEquals(0, results.size());
    results = Search.search("*", strategy, dsJoiner, true);
    assertEquals(0, results.size());
    results = Search.search("value*", strategy, dsJoiner, true);
    assertEquals(0, results.size());
    results = Search.search("", strategy, dsJoiner, true);
    assertEquals(0, results.size());

    strategy = SearchStrategy.parse(new ConfigThingy("", "Suchstrategie(test ())"));
    results = Search.search("value", strategy, dsJoiner, false);
    assertEquals(2, results.size());

    strategy = SearchStrategy.parse(new ConfigThingy("", "Suchstrategie(test ())"));
    results = Search.search("value", strategy, dsJoiner, true);
    assertEquals(2, results.size());

    strategy = SearchStrategy.parse(
        new ConfigThingy("", "Suchstrategie(test (column \"${suchanfrage1}\") test (column \"${suchanfrage1}3\"))"));
    dsJoiner = new DatasourceJoiner(Map.of("test", new MockDatasource("test", List.of("column", "column2"), List.of(
        new MockDataset("ds", Map.of("column", "value", "column2", "value2")),
        new MockDataset("ds2", Map.of("column", "value3", "column2", "value4")),
        new MockDataset("ds2", Map.of("column", "value3", "column2", "value4"))))), "test",
        new LocalOverrideStorageDummyImpl());
    results = Search.search("value", strategy, dsJoiner, true);
    assertEquals(2, results.size());
  }

  @Test
  void testSearch() throws Exception
  {
    DatasourceJoiner dsJoiner = new DatasourceJoiner(Map.of("test", new MockDatasource()), "test",
        new LocalOverrideStorageDummyImpl());
    assertEquals(2, Search.search(Collections.emptyMap(), dsJoiner).size());
    assertEquals(1, Search.search(Map.of("column", "value"), dsJoiner).size());
  }

  @Test
  void testHasLDAPDataChanged()
  {
    DatasourceJoiner dsJoiner = new DatasourceJoiner(Map.of("test", new MockDatasource()), "test",
        new LocalOverrideStorageDummyImpl());
    Dataset ds1 = new MockDataset();
    Dataset ds2 = new MockDataset("ds", "column", "value2");
    assertTrue(Search.hasLDAPDataChanged(ds1, ds2, dsJoiner));
    assertFalse(Search.hasLDAPDataChanged(ds1, ds1, dsJoiner));
    assertFalse(Search.hasLDAPDataChanged(null, null, null));
    assertFalse(Search.hasLDAPDataChanged(ds1, null, null));
    assertFalse(Search.hasLDAPDataChanged(ds1, ds2, null));

    Dataset ds3 = new MockDataset("ds", "column", null);
    assertTrue(Search.hasLDAPDataChanged(ds1, ds3, dsJoiner));
    // TODO: would expect true but is false
    // assertTrue(Search.hasLDAPDataChanged(ds3, ds1, dsJoiner));

    // TODO: would expect true but is false
    // Dataset ds4 = new MockDataset("ds", "column2", "value3");
    // assertTrue(Search.hasLDAPDataChanged(ds1, ds4, dsJoiner));
  }

}
