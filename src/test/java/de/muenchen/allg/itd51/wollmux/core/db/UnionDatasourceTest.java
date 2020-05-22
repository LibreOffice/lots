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
