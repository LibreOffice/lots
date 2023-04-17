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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDatasource;

public class PreferDatasourceTest
{

  @Test
  public void testPreferDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource());
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column"),
        List.of(new MockDataset("ds3", "column", "value3"), new MockDataset("ds", "column", "value4"))));
    Datasource ds = new PreferDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"prefer\" SOURCE \"mock\" OVER \"mock2\""), null);
    assertEquals("prefer", ds.getName());
    assertEquals(List.of("column"), ds.getSchema());
    QueryResults results = ds.getContents();
    assertEquals(0, results.size());
    results = ds.getDatasetsByKey(List.of("ds", "ds3"));
    assertEquals(2, results.size());
    results = ds.find(List.of(new QueryPart("column", "value3")));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
  }

  @Test
  public void testInvalidPreferDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource());
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column"), List.of()));
    assertThrows(ConfigurationErrorException.class,
        () -> new PreferDatasource(nameToDatasource, new ConfigThingy("", "NAME \"prefer\" SOURCE \"mock\""), null));
    assertThrows(ConfigurationErrorException.class,
        () -> new PreferDatasource(nameToDatasource, new ConfigThingy("", "NAME \"prefer\" OVER \"mock\""), null));
    assertThrows(ConfigurationErrorException.class, () -> new PreferDatasource(
        nameToDatasource,
        new ConfigThingy("", "NAME \"prefer\" SOURCE \"mock\" OVER \"unknown\""), null));
    assertThrows(ConfigurationErrorException.class, () -> new PreferDatasource(
        nameToDatasource,
        new ConfigThingy("", "NAME \"prefer\" SOURCE \"unknown\" OVER \"mock\""), null));

    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column1", "column2"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new PreferDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"prefer\" SOURCE \"mock\" OVER \"mock2\""), null));

    nameToDatasource.put("mock",
        new MockDatasource("mock", List.of("column1", "column2", "column3", "column4"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new PreferDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"prefer\" SOURCE \"mock\" OVER \"mock2\""), null));
  }

  @Test
  public void testPreferDatasourceResults() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource("mock", List.of("column"),
        List.of(new MockDataset("ds", "column", "value"))));
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column"),
        List.of(new MockDataset("ds", "column", "value3"), new MockDataset("ds1", "column", "value4"))));
    Datasource ds = new PreferDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"prefer\" SOURCE \"mock\" OVER \"mock2\""), null);
    QueryResults results = ds.getDatasetsByKey(List.of("ds"));
    assertFalse(results.isEmpty());
    assertEquals(1, results.size());
    Iterator<Dataset> iter = results.iterator();
    assertTrue(iter.hasNext());
    assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    Dataset data = iter.next();
    assertEquals("value", data.get("column"));
    assertFalse(iter.hasNext());
    assertThrows(NoSuchElementException.class, () -> iter.next());

    results = ds.getDatasetsByKey(List.of("unknown"));
    assertTrue(results.isEmpty());

    results = ds.getDatasetsByKey(List.of("ds1"));
    Iterator<Dataset> iter2 = results.iterator();
    data = iter2.next();
    assertEquals("value4", data.get("column"));
  }
}
