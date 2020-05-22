package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class OverlayDatasourceTest
{

  @Test
  void testOverlayDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock",
        new MockDatasource("mock", List.of("column", "join"),
            List.of(new MockDataset("ds", Map.of("column", "value", "join", "join")),
                new MockDataset("ds3", Map.of("column", "value3", "join", "nojoin")))));
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column2", "join"),
        List.of(new MockDataset("ds2", Map.of("column2", "value2", "join", "join")))));
    Datasource ds = new OverlayDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"overlay\" SOURCE \"mock\" OVERLAY \"mock2\" MODE \"so\" MATCH (\"join\" \"join\")"), null);
    assertEquals("overlay", ds.getName());
    assertEquals(List.of("column", "join", "column2", "join"), ds.getSchema());

    QueryResults results = ds.getContents();
    assertEquals(0, results.size());
    results = ds.getDatasetsByKey(List.of("ds"));
    assertEquals(1, results.size());
    results = ds.getDatasetsByKey(List.of("ds3"));
    assertEquals(1, results.size());

    results = ds.find(List.of(new QueryPart("column2", "value2")));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
    assertTrue(ds.find(List.of()).isEmpty());
  }

  @Test
  void testInvalidOverlayDatasource() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource());
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column2"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new OverlayDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" MODE \"so\" MATCH (\"column\", \"column2\")"), null));
    assertThrows(ConfigurationErrorException.class, () -> new AttachDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" OVERLAY \"mock\" MODE \"so\" MATCH (\"column\", \"column2\")"), null));
    assertThrows(ConfigurationErrorException.class,
        () -> new OverlayDatasource(nameToDatasource,
            new ConfigThingy("",
                "NAME \"attach\" SOURCE \"mock\" OVERLAY \"unknown\" MODE \"so\" MATCH (\"column\", \"column2\")"),
            null));
    assertThrows(ConfigurationErrorException.class,
        () -> new OverlayDatasource(nameToDatasource,
            new ConfigThingy("",
                "NAME \"attach\" SOURCE \"unknown\" OVERLAY \"mock\" MODE \"so\" MATCH (\"column\", \"column2\")"),
            null));

    assertThrows(ConfigurationErrorException.class,
        () -> new OverlayDatasource(nameToDatasource,
            new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" OVERLAY \"mock2\" MATCH (\"column\" \"column2\")"),
            null));
    assertThrows(ConfigurationErrorException.class,
        () -> new OverlayDatasource(nameToDatasource,
            new ConfigThingy("",
                "NAME \"attach\" SOURCE \"unknown\" OVERLAY \"mock\" MODE \"bla\" MATCH (\"column\", \"column2\")"),
            null));

    assertThrows(ConfigurationErrorException.class, () -> new OverlayDatasource(nameToDatasource,
        new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" OVERLAY \"mock2\" MODE \"so\""), null));
    assertThrows(ConfigurationErrorException.class,
        () -> new OverlayDatasource(nameToDatasource,
            new ConfigThingy("", "NAME \"attach\" SOURCE \"mock\" OVERLAY \"mock2\" MODE \"so\" MATCH (\"column\")"),
            null));
    assertThrows(ConfigurationErrorException.class, () -> new OverlayDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"attach\" SOURCE \"mock\" OVERLAY \"mock2\" MODE \"so\" MATCH (\"column\" \"unkown\")"), null));
    assertThrows(ConfigurationErrorException.class, () -> new OverlayDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"attach\" SOURCE \"mock\" OVERLAY \"mock2\" MODE \"so\" MATCH (\"unkown\" \"column2\")"), null));

    nameToDatasource.put("mock", new MockDatasource("mock", List.of("column1", "mock2__column2"), List.of()));
    assertThrows(ConfigurationErrorException.class, () -> new OverlayDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"union\" SOURCE \"mock\" ATTACH \"mock2\" MODE \"so\" MATCH (\"column\", \"column2\")"), null));
  }

  @Test
  void testConcatDatset() throws Exception
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock",
        new MockDatasource("mock", List.of("column", "join"),
            List.of(new MockDataset("ds", Map.of("column", "value", "join", "join")),
                new MockDataset("ds3", Map.of("column", "value3", "join", "nojoin")))));
    nameToDatasource.put("mock2", new MockDatasource("mock2", List.of("column", "column2", "join"),
        List.of(new MockDataset("ds2", Map.of("column", "value2", "column2", "value4", "join", "join")))));
    Datasource ds = new OverlayDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"overlay\" SOURCE \"mock\" OVERLAY \"mock2\" MODE \"so\" MATCH (\"join\" \"join\")"), null);
    QueryResults results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(0, results.size());
    results = ds.find(List.of(new QueryPart("column", "value2")));
    assertEquals(1, results.size());
    Dataset data = results.iterator().next();
    assertEquals("ds", data.getKey());
    assertEquals("value2", data.get("column"));
    assertThrows(ColumnNotFoundException.class, () -> data.get("unkown"));

    Dataset data2 = ds.getDatasetsByKey(List.of("ds3")).iterator().next();
    assertEquals("value3", data2.get("column"));
    assertNull(data2.get("column2"));

    ds = new OverlayDatasource(nameToDatasource, new ConfigThingy("",
        "NAME \"overlay\" SOURCE \"mock\" OVERLAY \"mock2\" MODE \"os\" MATCH (\"join\" \"join\")"), null);
    results = ds.find(List.of(new QueryPart("column", "value2")));
    assertEquals(0, results.size());
    results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
    Dataset data3 = results.iterator().next();
    assertEquals("ds", data3.getKey());
    assertEquals("value", data3.get("column"));
    assertThrows(ColumnNotFoundException.class, () -> data3.get("unkown"));
  }

}
