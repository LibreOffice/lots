/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.ColumnTransformer;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.Datasource;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDatasource;
import de.muenchen.allg.itd51.wollmux.func.StringLiteralFunction;
import de.muenchen.allg.itd51.wollmux.sender.mock.MockCache;

public class SenderServiceTest
{
  private SenderService service;
  private MockCache cache = null;
  private Dataset first;
  private Dataset second;

  @BeforeEach
  public void setup() throws URISyntaxException, SenderException
  {
    first = new MockDataset("ds", Map.of("column", "value1", "column2", "value2"));
    second = new MockDataset("ds2", Map.of("column", "value3", "column2", "value4"));
    Datasource ds = new MockDatasource("mock", List.of("column", "column2"), List.of(first, second));
    cache = new MockCache();
    service = new SenderService(ds, null, cache, "column");
  }

  @Test
  public void testCurrentSenderString() throws SenderException
  {
    assertEquals(", " + SenderService.SENDER_KEY_SEPARATOR + "ds", service.getCurrentSender());
    service.selectedSender = null;
    assertEquals("", service.getCurrentSender());
  }

  @Test
  public void testGetCurrentSenderValue() throws ColumnNotFoundException, SenderException
  {
    assertThrows(ColumnNotFoundException.class, () -> service.getCurrentSenderValue("unknown"));
    assertEquals(first.get("column"), service.getCurrentSenderValue("column"));
    service.selectedSender.dataset = null;
    assertEquals("", service.getCurrentSenderValue("column"));
  }

  @Test
  public void testGetCurrentSenderValues()
  {
    assertEquals(Map.of("column", "value1", "column2", "value2"), service.getCurrentSenderValues());
    service.selectedSender.dataset = null;
    assertEquals(Collections.emptyMap(), service.getCurrentSenderValues());
    service.selectedSender = null;
    assertEquals(Collections.emptyMap(), service.getCurrentSenderValues());
  }

  @Test
  public void testGetSelectedDatasetTransformed() throws Exception
  {
    Sender ds = service.getSelectedDatasetTransformed();
    assertEquals(first.get("column"), ds.get("column"));
    service.columnTransformer = new ColumnTransformer(
        Map.of("column", new StringLiteralFunction("column")));
    ds = service.getSelectedDatasetTransformed();
    assertEquals("column", ds.get("column"));
    assertEquals(first.get("column2"), ds.get("column2"));
  }

  @Test
  public void testGetPalEntries()
  {
    String[] pals = service.getPALEntries();
    assertEquals(2, pals.length);
    assertEquals(", " + SenderService.SENDER_KEY_SEPARATOR + "ds", pals[0]);
  }

  @Test
  public void testSaveCache() throws SenderException
  {
    service.notifyListener();
    assertTrue(cache.isSaved());
  }

  @Test
  public void testGetLostDatasets()
  {
    assertEquals(List.of(", "), service.getLostDatasetDisplayStrings());
  }

  @Test
  public void testSelect() throws SenderException
  {
    Sender sender = service.data.get(0);
    service.select(sender);
    assertTrue(sender.isSelected());
    assertEquals(service.selectedSender, sender);
    Sender s = new Sender(new MockDataset());
    assertThrows(SenderException.class, () -> service.select(s));
    service.select(null);
    assertNull(service.selectedSender);
  }

  @Test
  public void testSchema()
  {
    assertEquals(List.of("column", "column2"), service.getSchema());
  }

  @Test
  public void testFindListOfQueryParts() throws InterruptedException, ExecutionException
  {
    List<Sender> results = service.find(Map.of("column", "value1")).get();
    assertEquals(1, results.size());
  }

  @Test
  public void testGetAllSender()
  {
    assertEquals(2, service.getAllSender().size());
    service.data = null;
    assertEquals(Collections.emptyList(), service.getAllSender());
  }

  @Test
  public void testSenderListSorted()
  {
    List<Sender> sorted = service.getSenderListSorted("column");
    assertEquals("ds", sorted.get(0).getKey());
    assertNotNull(sorted.get(1).getKey());
  }

  @Test
  public void testNewSender()
  {
    Sender s = service.createNewSender();
    assertFalse(service.data.contains(s));
    assertEquals(Map.of("column", "column", "column2", "column2"), s.overridenValues);
  }

  @Test
  public void testSelectSender() throws SenderException
  {
    Sender selection = service.data.get(1);
    service.selectSender(service.getPALEntries()[1], 1);
    assertTrue(selection.isSelected());
    assertEquals(selection, service.getSelectedDatasetTransformed());
  }

  @Test
  public void testGetCurrentOverrideFragMap() throws Exception
  {
    assertThrows(SenderException.class, () -> service.getCurrentOverrideFragMap());
    String value = "(FRAG_ID \"A\" NEW_FRAG_ID \"B\")";
    service.columnTransformer = new ColumnTransformer(Map.of("column", new StringLiteralFunction(value)));
    ConfigThingy override = service.getCurrentOverrideFragMap();
    assertEquals("overrideFrag", override.getName());
    assertEquals(value, override.getFirstChild().stringRepresentation().trim());
    service.overrideFragDbSpalte = "unknown";
    assertEquals(0, service.getCurrentOverrideFragMap().count());
    service.overrideFragDbSpalte = "column";
    service.selectedSender = null;
    assertThrows(SenderException.class, () -> service.getCurrentOverrideFragMap());
    service.overrideFragDbSpalte = "";
    assertEquals(0, service.getCurrentOverrideFragMap().count());
  }

}
