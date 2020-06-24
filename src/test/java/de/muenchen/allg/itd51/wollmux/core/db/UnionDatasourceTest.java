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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDatasource;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

class UnionDatasourceTest
{

  @Test
  void testUnionDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource());
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column"),
        List.of(new MockDataset("ds3", "column", "value3"), new MockDataset("ds4", "column", "value4"))));
    Datasource ds = new UnionDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"union\" SOURCE1 \"mock\" SOURCE2 \"mock2\""), null);
    assertEquals("union", ds.getName());
    assertEquals(List.of("column"), ds.getSchema());
    QueryResults results = ds.getContents();
    assertEquals(0, results.size());
    results = ds.getDatasetsByKey(List.of("ds", "ds3"));
    assertEquals(2, results.size());
    results = ds.find(List.of(new QueryPart("column", "value4")));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
  }

  @Test
  void testInvalidUnionDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource());
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column"), List.of()));
    assertThrows(ConfigurationErrorException.class,
        () -> new UnionDatasource(nameToDatasource, new ConfigThingy("", "NAME \"union\" SOURCE1 \"mock\""), null));
    assertThrows(ConfigurationErrorException.class,
        () -> new UnionDatasource(nameToDatasource, new ConfigThingy("", "NAME \"union\" SOURCE2 \"mock\""), null));
    assertThrows(ConfigurationErrorException.class, () -> new UnionDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"union\" SOURCE1 \"mock\" SOURCE2 \"unknown\""), null));
    assertThrows(ConfigurationErrorException.class, () -> new UnionDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"union\" SOURCE1 \"unknown\" SOURCE2 \"mock\""), null));

    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column1", "column2"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new UnionDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"union\" SOURCE1 \"mock\" SOURCE2 \"mock2\""), null));

    nameToDatasource.put("mock",
        new MockDatasource("mock", List.of("column1", "column2", "column3", "column4"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new UnionDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"union\" SOURCE1 \"mock\" SOURCE2 \"mock2\""), null));
  }

}
