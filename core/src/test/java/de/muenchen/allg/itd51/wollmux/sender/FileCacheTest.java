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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;

class FileCacheTest
{
  private URL file = FileCacheTest.class.getResource("cache.conf");

  @Test
  void testSchema() throws URISyntaxException
  {
    FileCache cache = new FileCache(Paths.get(file.toURI()).toFile(), null);
    assertEquals(List.of("column", "column2"), cache.getSchema());
  }

  @Test
  void testSelectedKey() throws URISyntaxException
  {
    FileCache cache = new FileCache(Paths.get(file.toURI()).toFile(), null);
    assertEquals("ds", cache.getSelectedKey());
  }

  @Test
  void testSelectedKeyIndex() throws URISyntaxException
  {
    FileCache cache = new FileCache(Paths.get(file.toURI()).toFile(), null);
    assertEquals(0, cache.getSelectedSameKeyIndex());
  }

  @Test
  void testData() throws URISyntaxException
  {
    FileCache cache = new FileCache(Paths.get(file.toURI()).toFile(), null);
    List<SenderConf> dataList = cache.getData();
    assertEquals(1, dataList.size());
    SenderConf data = dataList.get(0);
    assertEquals("ds", data.getKey());
    assertEquals(Map.of("column", "value1", "column2", "value2"), data.getCachedValues());
    assertEquals(Map.of("column2", "override2"), data.getOverriddenValues());
  }

  @Test
  void testCreateConfig() throws Exception
  {
    try (Reader reader = new InputStreamReader(new FileInputStream(Paths.get(file.toURI()).toFile()),
        StandardCharsets.UTF_8))
    {
      ConfigThingy conf = new ConfigThingy(Paths.get(file.toURI()).toFile().getPath(), null, reader);
      Sender sender = new Sender(null, new MockDataset("ds", Map.of("column", "value1", "column2", "value2")),
          Map.of("column2", "override2"));
      FileCache cache = new FileCache(Paths.get(file.toURI()).toFile(), null);
      ConfigThingy newConf = cache.createCacheData(cache.getSchema(), sender, List.of(sender));
      assertEquals(conf.stringRepresentation(), newConf.stringRepresentation());
    }
  }
  
  @Test
  void testCreateConfigNoSelected() throws Exception
  {
    try (Reader reader = new InputStreamReader(new FileInputStream(Paths.get(file.toURI()).toFile()),
        StandardCharsets.UTF_8))
    {
      Sender sender = new Sender(null, new MockDataset("ds", Map.of("column", "value1", "column2", "value2")),
          Map.of("column2", "override2"));
      FileCache cache = new FileCache(Paths.get(file.toURI()).toFile(), null);
      ConfigThingy newConf = cache.createCacheData(cache.getSchema(), null, List.of(sender));
      assertEquals(0, newConf.get("Ausgewaehlt").count());
    }
  }

  @Test
  void testCreateConfigNoSchema() throws Exception
  {
    FileCache cache = new FileCache(Paths.get(file.toURI()).toFile(), null);
    assertThrows(SenderException.class, () -> cache.createCacheData(null, null, Collections.emptyList()));
  }

  @Test
  void testFileNotFound() throws Exception
  {
    FileCache cache = new FileCache(new File("unknown"), null);
    assertTrue(cache.getSchema().isEmpty());
    assertTrue(cache.getData().isEmpty());
    assertNull(cache.getSelectedKey());
    assertEquals(-1, cache.getSelectedSameKeyIndex());
  }

}
