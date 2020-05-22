package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.test.OfficeTest;

class OOoDatasourceTest extends OfficeTest
{

  @Test
  void testOOoDatasource() throws Exception
  {
    Datasource ds = new OOoDatasource(null,
        new ConfigThingy("", "NAME \"ooo\" SOURCE \"Bibliography\" TABLE \"biblio\" Schluessel (\"Identifier\")"));
    assertEquals("ooo", ds.getName());
    assertFalse(ds.getSchema().isEmpty());

    QueryResults results = ds.getContents();
    assertEquals(20, results.size());
    results = ds.getDatasetsByKey(List.of("Identifier#ARJ00#"));
    assertEquals(1, results.size());
    Dataset data = results.iterator().next();
    assertEquals("Identifier#ARJ00#", data.getKey());
    assertEquals("99", data.get("Pages"));
    assertThrows(ColumnNotFoundException.class, () -> data.get("unknown"));

    results = ds.find(List.of(new QueryPart("Author", "Gris, Myriam")));
    assertEquals(5, results.size());
  }

}
