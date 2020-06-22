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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl.LOSDJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDatasource;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class LocalOverrideStorageStandardImplTest
{
  URL cache = LocalOverrideStorageStandardImplTest.class.getResource("cache.conf");
  LocalOverrideStorage los;

  @BeforeEach
  void setup() throws Exception
  {
    los = new LocalOverrideStorageStandardImpl(Paths.get(cache.toURI()).toFile(), null);
  }

  @Test
  void testData() throws Exception
  {
    assertFalse(los.isEmpty());

    ConfigThingy dump = new ConfigThingy("Daten");
    los.dumpData(dump);
    // TODO ConfigThingy should implement equals and hashcode
    assertEquals(new ConfigThingy("", cache).get("Daten").stringRepresentation(), dump.stringRepresentation());

    assertEquals(0, los.getSelectedDatasetSameKeyIndex());
    DJDataset ds = los.getSelectedDataset();
    assertNotNull(ds);
    assertEquals("ds", ds.getKey());
    assertEquals("value2", ds.get("column2"));
    assertTrue(ds.isSelectedDataset());
    ds = ds.copy();

    los.copyNonLOSDataset(new MockDataset());
    assertEquals(3, los.size());

    LOSDJDataset losDJ = (LOSDJDataset) los.getSelectedDataset();
    assertFalse(losDJ.isDifferentFromLdapDataset("column", losDJ));
  }

  @Test
  void testNewDataset()
  {
    ((DJDataset) los.iterator().next()).remove();
    DJDataset newDs = los.newDataset();
    assertTrue(newDs.isSelectedDataset());
    newDs = los.newDataset();
    assertFalse(newDs.isSelectedDataset());
  }

  @Test
  void testSchema()
  {
    assertEquals(List.of("column", "column2"), los.getSchema());
    los.setSchema(List.of("dummy"));
    assertEquals(List.of("dummy"), los.getSchema());
    los.setSchema(List.of("dummy"));
    assertEquals(List.of("dummy"), los.getSchema());
  }

  @Test
  void testRefreshFromDatabase() throws Exception
  {
    los.refreshFromDatabase(new MockDatasource());
    assertEquals(1, los.size());
    assertEquals("value", los.getSelectedDataset().get("column"));
  }

}
