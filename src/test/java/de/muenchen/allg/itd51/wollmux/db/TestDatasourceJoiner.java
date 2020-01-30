package de.muenchen.allg.itd51.wollmux.db;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageDummyImpl;
import de.muenchen.allg.itd51.wollmux.core.db.QueryPart;

public class TestDatasourceJoiner
{
  private static DatasourceJoiner dsJoiner = null;

  @BeforeAll
  public static void setupTest()
  {
    dsJoiner = new DatasourceJoiner(Collections.emptyMap(), "test",
        new LocalOverrideStorageDummyImpl());
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