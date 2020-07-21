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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DJDatasetBaseTest
{
  private DJDatasetBase dataset;
  private Map<String, String> backingStore;
  private Map<String, String> overrideStore;

  @BeforeEach
  void setup()
  {
    backingStore = Map.of("Rolle", "value1", "Vorname", "value2");
    overrideStore = Map.of("Rolle", "override1", "Nachname", "value3");
    List<String> schema = List.of("Rolle", "Vorname", "Nachname");
    dataset = new DJDatasetBase(backingStore, overrideStore, schema)
    {

      @Override
      public String getKey()
      {
        return null;
      }

      @Override
      public void select()
      {
      }

      @Override
      public void remove()
      {
      }

      @Override
      public boolean isSelectedDataset()
      {
        return false;
      }

      @Override
      public DJDataset copy()
      {
        return null;
      }
    };
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
