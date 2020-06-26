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

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockDatasource;
import de.muenchen.allg.itd51.wollmux.db.mock.MockQueryResults;
import de.muenchen.allg.itd51.wollmux.func.StringLiteralFunction;

public class DatasourceJoinerTest
{
  private static DatasourceJoiner dsJoiner = null;

  @BeforeAll
  public static void setupTest()
  {
    Map<String, Datasource> nameToDatasource = new HashMap<>();
    nameToDatasource.put("mock", new MockDatasource());
    dsJoiner = new DatasourceJoiner(nameToDatasource, "mock",
        new LocalOverrideStorageDummyImpl());
  }

  @Test
  void testBasics()
  {
    assertNotNull(dsJoiner.getMainDatasource());
    assertNotNull(dsJoiner.getDatasource("mock"));
    assertEquals(List.of("column"), dsJoiner.getMainDatasourceSchema());
    assertEquals(2, dsJoiner.getContentsOfMainDatasource().size());
    assertEquals(2, dsJoiner.getContentsOf("mock").size());
    assertThrows(IllegalArgumentException.class, () -> dsJoiner.getContentsOf("unknown"));
    assertTrue(dsJoiner.getLostDatasets().isEmpty());
    dsJoiner.setLostDatasets(List.of(new MockDataset()));
    assertFalse(dsJoiner.getLostDatasets().isEmpty());
    assertEquals(1, dsJoiner.getLOS().size());
  }

  @Test
  void testDJDatasetWrapper() throws Exception
  {
    DJDataset ds = (DJDataset) dsJoiner.getContentsOfMainDatasource().iterator().next();
    assertThrows(UnsupportedOperationException.class, () -> ds.set("column2", "value2"));
    assertThrows(UnsupportedOperationException.class, () -> ds.select());
    assertThrows(UnsupportedOperationException.class, () -> ds.remove());
    assertFalse(ds.hasLocalOverride("column"));
    assertFalse(ds.isFromLOS());
    assertFalse(ds.isSelectedDataset());
    assertTrue(ds.hasBackingStore());
    ds.discardLocalOverride("column");
    assertEquals("value", ds.get("column"));
    assertEquals("ds", ds.getKey());
    assertEquals("", ds.toString());
  }

  @Test
  void testInit()
  {
    DatasourceJoiner joiner = new DatasourceJoiner();
    joiner.init(Collections.emptyMap(), "test", new LocalOverrideStorageDummyImpl());
    assertTrue(joiner.getMainDatasource() instanceof EmptyDatasource);

    joiner.init(Collections.emptyMap(), DatasourceJoiner.NOCONFIG, new LocalOverrideStorageDummyImpl());
    assertTrue(joiner.getMainDatasource() instanceof DummyDatasourceWithMessagebox);

    Map<String, Datasource> datasources = new HashMap<>();
    datasources.put("test", null);
    joiner.init(datasources, "test2", new LocalOverrideStorageDummyImpl());
    assertNull(joiner.getDatasource("test"));

    assertThrows(ConfigurationErrorException.class,
        () -> joiner.init(Collections.emptyMap(), "test3", new LocalOverrideStorageDummyImpl()
        {
          @Override
          public List<String> getSchema()
          {
            return null;
          }
        }));
  }

  @Test
  void testFindQuery()
  {
    assertThrows(IllegalArgumentException.class, () -> dsJoiner.find(new Query("unknown", List.of())));
    assertThrows(IllegalArgumentException.class,
        () -> dsJoiner.find(new Query("mock", List.of(new QueryPart("column", "*foo*bar*")))));
    assertThrows(IllegalArgumentException.class,
        () -> dsJoiner.find(new Query("mock", List.of(new QueryPart("column", null)))));
    QueryResults results = dsJoiner.find(new Query("mock", List.of(new QueryPart("column", "value"))));
    assertEquals(1, results.size());
  }

  @Test
  void testFindListOfQueryParts()
  {
    QueryResults results = dsJoiner.find(List.of(new QueryPart("column", "value")));
    assertEquals(1, results.size());
  }

  @Test
  void testSaveCacheAndLOS() throws Exception
  {
    ConfigThingy conf = dsJoiner.saveCacheAndLOS(Files.createTempFile("", "").toFile());
    assertEquals(3, conf.count());
  }

  @Test
  void testAddToPAL()
  {
    assertEquals(0, dsJoiner.addToPAL(null));
    assertEquals(0, dsJoiner.addToPAL(new MockQueryResults(new Dataset[] {})));
    assertEquals(1, dsJoiner.addToPAL(new MockQueryResults()));
  }

  @Test
  void testGetSelectedDatasetTransformed() throws Exception
  {
    Dataset ds = dsJoiner.getSelectedDatasetTransformed();
    assertEquals("column", ds.get("column"));
    dsJoiner.setTransformer(new ColumnTransformer(Map.of("column", new StringLiteralFunction("value"))));
    ds = dsJoiner.getSelectedDatasetTransformed();
    assertEquals("value", ds.get("column"));
  }

  @Test
  public void testBuildQueryValid1Entry()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(new ImmutablePair<>("Vorname", "Max"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 1);
  }

  @Test
  public void testBuildQueryInValidEntryStar()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(new ImmutablePair<>("Vorname", "*"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.isEmpty());
  }

  @Test
  public void testBuildQueryInValidEntryStarAndValidElements()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(new ImmutablePair<>("Vorname", "*"));
    query.add(new ImmutablePair<>("Nachname", "Mustermann"));
    query.add(new ImmutablePair<>("Ort", "Testort"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 2);
  }

  @Test
  public void testBuildQueryValid2Entries()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(new ImmutablePair<>("Vorname", "Max"));
    query.add(new ImmutablePair<>("Nachname", "Mustermann"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 2);
  }

  @Test
  public void testBuildQueryValid3Entries()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(new ImmutablePair<>("Vorname", "Max"));
    query.add(new ImmutablePair<>("Nachname", "Mustermann"));
    query.add(new ImmutablePair<>("Ort", "Testort"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 3);
  }

  @Test
  public void testBuildQueryInValidAssertsNullPointerException()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(new ImmutablePair<>(null, "Max"));
    query.add(new ImmutablePair<>("Nachname", "Mustermann"));

    assertThrows(NullPointerException.class, () -> dsJoiner.buildQuery(query));
  }

  @Test
  public void testBuildQueryOneInvalidEntry()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(new ImmutablePair<>("Vorname", null));
    query.add(new ImmutablePair<>("Nachname", "Mustermann"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 1);
  }

  @Test
  public void testBuildQueryTwoInvalidEntry()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(new ImmutablePair<>("Vorname", null));
    query.add(new ImmutablePair<>("Nachname", null));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.isEmpty());
  }

}