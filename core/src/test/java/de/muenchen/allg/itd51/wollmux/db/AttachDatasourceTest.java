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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDatasource;

public class AttachDatasourceTest
{

  @Test
  public void testAttachDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource("mock", List.of("column", "join"),
        List.of(new MockDataset("ds", Map.of("column", "value", "join", "join")),
            new MockDataset("ds3", Map.of("column", "value3", "join", "nojoin")))));
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column2", "join"),
        List.of(new MockDataset("ds2", Map.of("column2", "value2", "join", "join")))));
    Datasource ds = new AttachDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" ATTACH \"mock2\" MATCH (\"join\", \"join\")"), null);
    assertEquals("attach", ds.getName());
    assertEquals(List.of("column", "join", "mock2__column2", "mock2__join"), ds.getSchema());
    QueryResults results = ds.getContents();
    assertEquals(0, results.size());
    results = ds.getDatasetsByKey(List.of("ds"));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("mock2__column2", "value2")));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
  }

  @Test
  public void testInvalidAttachDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource());
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column2"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(
        nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" MATCH (\"column\", \"column2\")"), null));
    assertThrows(ConfigurationErrorException.class,
        () -> new AttachDatasource(nameToDatasource,
            new ConfigThingy("", "NAME \"attach\" ATTACH \"mock\" MATCH (\"column\", \"column2\")"), null));
    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(nameToDatasource,
        new ConfigThingy("",
            "NAME \"attach\" SOURCE \"mock\" ATTACH \"unknown\" MATCH (\"column\", \"column2\")"),
        null));
    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(nameToDatasource,
        new ConfigThingy("",
            "NAME \"attach\" SOURCE \"unknown\" ATTACH \"mock\" MATCH (\"column\", \"column2\")"),
        null));

    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" ATTACH \"mock2\""), null));
    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" ATTACH \"mock2\" MATCH (\"column\")"), null));
    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" ATTACH \"mock2\" MATCH (\"column\" \"unkown\")"), null));
    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" ATTACH \"mock2\" MATCH (\"unkown\" \"column2\")"), null));

    nameToDatasource.put("mock", new MockDatasource("mock", List.of("column1", "mock2__column2"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(nameToDatasource,
        new ConfigThingy("",
            "NAME \"union\" SOURCE \"mock\" ATTACH \"mock2\" MATCH (\"column\", \"column2\")"),
        null));
  }

  @Test
  public void testConcatDatset() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock",
        new MockDatasource("mock", List.of("column", "join"),
            List.of(new MockDataset("ds", Map.of("column", "value", "join", "join")),
                new MockDataset("ds3", Map.of("column", "value3", "join", "nojoin")))));
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column2", "join"),
        List.of(new MockDataset("ds2", Map.of("column2", "value2", "join", "join")))));
    Datasource ds = new AttachDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" ATTACH \"mock2\" MATCH (\"join\", \"join\")"), null);
    QueryResults results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
    Dataset data = results.iterator().next();
    assertEquals("ds", data.getKey());
    assertEquals("value", data.get("column"));
    assertEquals("value2", data.get("mock2__column2"));
    assertThrows(ColumnNotFoundException.class, () -> data.get("unkown"));

    Dataset data2 = ds.find(List.of(new QueryPart("column", "value3"))).iterator().next();
    assertNull(data2.get("mock2__column2"));
  }

}
