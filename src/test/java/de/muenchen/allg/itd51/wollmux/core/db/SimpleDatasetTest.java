package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SimpleDatasetTest
{

  @Test
  void testSimpleDataset() throws Exception
  {
    Map<String, String> data = new HashMap<>();
    data.put("column", "value");
    Dataset dataset = new SimpleDataset("Test", data);

    assertEquals("Test", dataset.getKey());
    assertEquals("value", dataset.get("column"));
    assertThrows(ColumnNotFoundException.class, () -> dataset.get("column2"));

    Dataset dataset2 = new SimpleDataset(List.of("column"), dataset);
    assertEquals("Test", dataset2.getKey());
    assertEquals("value", dataset2.get("column"));
  }

}
