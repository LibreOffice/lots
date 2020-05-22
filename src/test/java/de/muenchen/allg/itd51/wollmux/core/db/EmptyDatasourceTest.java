package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class EmptyDatasourceTest
{

  @Test
  void testEmptyDatasource()
  {
    List<String> schema = List.of("column");
    Datasource empty = new EmptyDatasource(schema, "empty");
    assertEquals(schema, empty.getSchema());
    assertEquals("empty", empty.getName());
    assertTrue(empty.getDatasetsByKey(List.of("test")).isEmpty());
    assertTrue(empty.getContents().isEmpty());
    assertTrue(empty.find(Collections.emptyList()).isEmpty());
  }

}
