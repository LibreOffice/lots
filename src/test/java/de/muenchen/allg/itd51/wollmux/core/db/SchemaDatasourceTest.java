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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDatasource;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

class SchemaDatasourceTest
{

  @Test
  void testSchemaDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource("mock2", List.of("column", "column2", "column3"),
        List.of(new MockDataset("ds", Map.of("column", "value", "column2", "value2", "column3", "value3")))));
    Datasource ds = new SchemaDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"schema\" SOURCE \"mock\" DROP (\"column\") RENAME (\"column2\" \"renameColumn\") ADD (\"newColumn\")"),
        null);
    assertEquals("schema", ds.getName());
    assertEquals(List.of("column3", "newColumn", "renameColumn"), ds.getSchema());
    QueryResults results = ds.getContents();
    assertEquals(0, results.size());
    results = ds.getDatasetsByKey(List.of("ds"));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("renameColumn", "value2")));
    assertEquals(1, results.size());
    Dataset data = results.iterator().next();
    assertEquals("ds", data.getKey());
    assertThrows(ColumnNotFoundException.class, () -> data.get("column"));
    assertEquals("value2", data.get("renameColumn"));
    assertEquals("value3", data.get("column3"));
    assertNull(data.get("newColumn"));

    assertTrue(ds.find(List.of(new QueryPart("unkown", "value"))).isEmpty());
    assertTrue(ds.find(List.of(new QueryPart("newColumn", "value"))).isEmpty());
    assertFalse(ds.find(List.of(new QueryPart("column3", "value3"))).isEmpty());
  }

  @Test
  void testInvalidSchemaDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource("mock2", List.of("column", "column2", "column3"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new SchemaDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"schema\" DROP (\"column\") RENAME (\"column2\" \"renameColumn\") ADD (\"newColumn\")"),
        null));
    assertThrows(ConfigurationErrorException.class, () -> new SchemaDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"schema\" SOURCE \"unkown\" DROP (\"column\") RENAME (\"column2\" \"renameColumn\") ADD (\"newColumn\")"),
        null));

    assertThrows(ConfigurationErrorException.class, () -> new SchemaDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"schema\" SOURCE \"mock\" RENAME (\"column2\")"), null));
    assertThrows(ConfigurationErrorException.class, () -> new SchemaDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"schema\" SOURCE \"mock\" RENAME (\"unkown\" \"renameColumn\")"),
        null));
    assertThrows(ConfigurationErrorException.class, () -> new SchemaDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"schema\" SOURCE \"mock\" RENAME (\"column2\" \"foo&bar\")"), null));

    assertThrows(ConfigurationErrorException.class, () -> new SchemaDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"schema\" SOURCE \"mock\" ADD (\"foo&bar\")"), null));

    assertThrows(ConfigurationErrorException.class, () -> new SchemaDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"schema\" SOURCE \"mock\" DROP (\"unkown\")"), null));
  }

}
