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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.LocalOverrideStorageStandardImpl.LOSDJDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDatasource;

class LocalOverrideStorageStandardImplTest
{
  URL cache = LocalOverrideStorageStandardImplTest.class.getResource("cache.conf");
  private LocalOverrideStorageStandardImpl los;
  private LOSDJDataset dataset;
  private Map<String, String> backingStore;
  private Map<String, String> overrideStore;

  @BeforeEach
  void setup() throws Exception
  {
    los = new LocalOverrideStorageStandardImpl(Paths.get(cache.toURI()).toFile(), null);
    backingStore = Map.of("Rolle", "value1", "Vorname", "value2");
    overrideStore = Map.of("Rolle", "override1", "Nachname", "value3");
    List<String> schema = List.of("Rolle", "Vorname", "Nachname");
    dataset = los.new LOSDJDataset(backingStore, overrideStore, schema, "key");
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

  @Test
  void testDatasetToString()
  {
    assertEquals("(override1) value3, value2", dataset.toString());
    dataset.myLOS = Collections.emptyMap();
    dataset.myBS = Collections.emptyMap();
    assertEquals(", ", dataset.toString());
    dataset.myBS = Map.of("Rolle", "", "Vorname", "", "Nachname", "");
    assertEquals(", ", dataset.toString());
  }

  @Test
  void testDatasetGetValue() throws ColumnNotFoundException
  {
    assertEquals("override1", dataset.get("Rolle"));
    assertEquals("value2", dataset.get("Vorname"));
    assertEquals("value3", dataset.get("Nachname"));
    assertThrows(ColumnNotFoundException.class, () -> dataset.get("unknown"));
    dataset.schema = null;
    dataset.myBS = null;
    assertNull(dataset.get("unknown"));
  }

  @Test
  void testDatasetSetValue() throws ColumnNotFoundException
  {
    dataset.myLOS = new HashMap<>();
    dataset.set("Rolle", "test");
    assertEquals("test", dataset.get("Rolle"));
    assertThrows(IllegalArgumentException.class, () -> dataset.set("test", null));
    dataset.myLOS = null;
    assertThrows(UnsupportedOperationException.class, () -> dataset.set("test", "test"));
  }

  @Test
  void testDatasetDiscardLocalOverride() throws ColumnNotFoundException, NoBackingStoreException
  {
    dataset.myLOS = new HashMap<>();
    dataset.myLOS.put("Rolle", "test");
    dataset.discardLocalOverride("Rolle");
    assertEquals("value1", dataset.get("Rolle"));
    dataset.myBS = null;
    assertThrows(NoBackingStoreException.class, () -> dataset.discardLocalOverride("Rolle"));
    dataset.myLOS = null;
    dataset.discardLocalOverride("Rolle");
  }

  @Test
  void testDatasetHasLocalOverride() throws ColumnNotFoundException
  {
    assertTrue(dataset.hasLocalOverride("Rolle"));
    assertFalse(dataset.hasLocalOverride("Vorname"));
    dataset.myBS = null;
    assertTrue(dataset.hasLocalOverride("unknown")); // TODO: should be false
  }

}
