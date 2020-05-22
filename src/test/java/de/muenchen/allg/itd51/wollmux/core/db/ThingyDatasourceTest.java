package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class ThingyDatasourceTest
{
  URL file = getClass().getResource("thingyDatasource.conf");

  @Test
  void testThingyDatasource() throws Exception
  {
    Datasource ds = new ThingyDatasource(null,
        new ConfigThingy("", "NAME \"conf\" URL \"" + file + "\" Schluessel(\"column\" \"column2\")"), null);
    Dataset data = ds.find(List.of(new QueryPart("column", "value1"))).iterator().next();
    assertTrue(data.getKey().contains("value1") && data.getKey().contains("value2"));
    assertEquals("value2", data.get("column2"));
    assertThrows(ColumnNotFoundException.class, () -> data.get("unknown"));
  }

}
