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
package de.muenchen.allg.itd51.wollmux.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;

public class RAMDatasourceTest
{

  @Test
  public void testRAMDatasource()
  {
    Datasource ds = new RAMDatasource("ram", List.of("column"),
        List.of(new MockDataset(), new MockDataset("ds3", "column", "value3")));
    assertEquals(List.of("column"), ds.getSchema());
    assertEquals("ram", ds.getName());
    QueryResults results = ds.getContents();
    assertEquals(2, results.size());
    results = ds.getDatasetsByKey(List.of("ds", "ds2"));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
    results = ds.find(List.of());
    assertEquals(0, results.size());
    results = ds.find(List.of(new QueryPart("column", "value2")));
    assertEquals(0, results.size());
  }

  @Test
  public void testUninitialized()
  {
    Datasource ds = new RAMDatasource();
    assertNull(ds.getName());
    assertThrows(NullPointerException.class, () -> ds.getSchema());
  }

}
