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
package de.muenchen.allg.itd51.wollmux.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;

public class SenderTest
{
  private Sender sender;
  private Dataset ds;
  private Map<String, String> overrideStore;

  @BeforeEach
  public void setup() throws URISyntaxException
  {
    ds = new MockDataset("key", Map.of("Rolle", "value1", "Vorname", "value2"));
    overrideStore = new HashMap<>();
    overrideStore.put("Rolle", "override1");
    overrideStore.put("Nachname", "value3");
    sender = new Sender(null, ds, overrideStore);
  }

  @Test
  void testSenderWithoutOverride()
  {
    sender = new Sender(ds);
    assertEquals(Collections.emptyMap(), sender.getOverridenValues());
    assertEquals(ds, sender.getDataset());
  }

  @Test
  void testGetKey()
  {
    assertEquals("key", sender.getKey());
    sender = new Sender(overrideStore);
    assertNotNull(sender.getKey());
  }

  @Test
  void testDatasetToString()
  {
    assertEquals("(override1) value3, value2", sender.getDisplayString());
    sender.overridenValues = Collections.emptyMap();
    Map<String, String> ds = new HashMap<>();
    ds.put("Rolle", null);
    ds.put("Vorname", null);
    ds.put("Nachname", null);
    sender.dataset = new MockDataset("", ds);
    assertEquals(", ", sender.getDisplayString());
    sender.dataset = new MockDataset("", Map.of("Rolle", "", "Vorname", "", "Nachname", ""));
    assertEquals(", ", sender.getDisplayString());
  }

  @Test
  void testHasBackingStore()
  {
    assertTrue(sender.isFromDatabase());
    sender.dataset = null;
    assertFalse(sender.isFromDatabase());
  }

  @Test
  void testDatasetGetValue() throws ColumnNotFoundException
  {
    assertEquals("override1", sender.get("Rolle"));
    assertEquals("value2", sender.get("Vorname"));
    assertEquals("value3", sender.get("Nachname"));
    assertEquals(null, sender.get("unknown"));
    sender.dataset = null;
    assertNull(sender.get("unknown"));
  }

  @Test
  void testDatasetSetValue() throws ColumnNotFoundException, SenderException
  {
    sender.overridenValues = new HashMap<>();
    sender.overrideValue("Rolle", "test");
    assertEquals("test", sender.get("Rolle"));
    assertThrows(SenderException.class, () -> sender.overrideValue("test", null));
  }

  @Test
  void testDropColumn()
  {
    sender.drop("Rolle");
    assertEquals("value1", sender.get("Rolle"));
  }

  @Test
  void testCopySender()
  {
    Sender copy = new Sender(sender);
    assertEquals(sender.getKey(), copy.getKey());

    sender.dataset = null;
    copy = new Sender(sender);
    assertEquals(sender.getKey(), copy.getKey());
  }

  @Test
  void testIsOverriden()
  {
    assertTrue(sender.isOverriden());
  }

  @Test
  void testIsOverridenWithoutOverrideStore()
  {
    sender.overridenValues = null;
    assertFalse(sender.isOverriden());
  }

  @Test
  void testIsOverridenWithEmptyOverrideValue()
  {
    sender.overridenValues = new HashMap<>();
    sender.overridenValues.put("Rolle", "");
    assertFalse(sender.isOverriden());
    sender.overridenValues.put("Rolle", null);
    assertFalse(sender.isOverriden());
  }

  @Test
  void testIsOverrrideNotFromDatabase()
  {
    sender.dataset = null;
    assertFalse(sender.isOverriden());
  }

  @Test
  void testIsOverridenSameValues()
  {
    sender.overridenValues = new HashMap<>();
    sender.overridenValues.put("Rolle", "value1");
    assertFalse(sender.isOverriden());
  }

  @Test
  void testIsOverridenDifferentValues()
  {
    sender.overridenValues = new HashMap<>();
    sender.overridenValues.put("Rolle", "override");
    assertTrue(sender.isOverriden());
  }

  @Test
  void testIsSelected()
  {
    assertFalse(sender.isSelected());
    sender.setSelected(true);
    assertTrue(sender.isSelected());
  }

  @Test
  void testHashCode()
  {
    assertEquals(Objects.hash(ds, ds.getKey(), overrideStore, false), sender.hashCode());
  }

  @Test
  void testEquals()
  {
    assertTrue(sender.equals(sender));
    assertFalse(sender.equals(null));
    assertFalse(sender.equals(new Object()));
    Sender copy = new Sender(sender);
    assertTrue(sender.equals(copy));
    copy.selected = true;
    assertFalse(sender.equals(copy));
    copy.overridenValues = null;
    assertFalse(sender.equals(copy));
    copy.key = null;
    assertFalse(sender.equals(copy));
    copy.dataset = null;
    assertFalse(sender.equals(copy));
  }

  @Test
  void testComparator()
  {
    Sender copy = new Sender(sender);
    assertEquals(0, Sender.comparatorByColumn("Rolle").compare(sender, copy));
    copy.overridenValues.put("Rolle", "a");
    assertTrue(Sender.comparatorByColumn("Rolle").compare(sender, copy) > 0);
    copy.overridenValues.put("Rolle", "z");
    assertTrue(Sender.comparatorByColumn("Rolle").compare(sender, copy) < 0);
    copy.overridenValues.put("Rolle", null);
    assertTrue(Sender.comparatorByColumn("Rolle").compare(sender, copy) < 0);
    sender.overridenValues.put("Nachname", null);
    assertTrue(Sender.comparatorByColumn("Nachname").compare(sender, copy) > 0);
    copy.overridenValues.put("Nachname", null);
    assertEquals(0, Sender.comparatorByColumn("Nachname").compare(sender, copy));
  }

}