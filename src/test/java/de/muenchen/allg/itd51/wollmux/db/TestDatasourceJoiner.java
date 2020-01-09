package de.muenchen.allg.itd51.wollmux.db;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.javatuples.Pair;
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
    query.add(Pair.with("Vorname", "Max"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 1);
  }

  @Test
  public void testBuildQueryInValidEntryStar()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(Pair.with("Vorname", "*"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.isEmpty());
  }

  @Test
  public void testBuildQueryInValidEntryStarAndValidElements()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(Pair.with("Vorname", "*"));
    query.add(Pair.with("Nachname", "Mustermann"));
    query.add(Pair.with("Ort", "Testort"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 2);
  }

  @Test
  public void testBuildQueryValid2Entries()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(Pair.with("Vorname", "Max"));
    query.add(Pair.with("Nachname", "Mustermann"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 2);
  }

  @Test
  public void testBuildQueryValid3Entries()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(Pair.with("Vorname", "Max"));
    query.add(Pair.with("Nachname", "Mustermann"));
    query.add(Pair.with("Ort", "Testort"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 3);
  }

  @Test
  public void testBuildQueryInValidAssertsNullPointerException()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(Pair.with(null, "Max"));
    query.add(Pair.with("Nachname", "Mustermann"));

    assertThrows(NullPointerException.class, () -> dsJoiner.buildQuery(query));
  }

  @Test
  public void testBuildQueryOneInvalidEntry()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(Pair.with("Vorname", null));
    query.add(Pair.with("Nachname", "Mustermann"));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.size() == 1);
  }

  @Test
  public void testBuildQueryTwoInvalidEntry()
  {
    List<Pair<String, String>> query = new ArrayList<>();
    query.add(Pair.with("Vorname", null));
    query.add(Pair.with("Nachname", null));

    List<QueryPart> queryParts = dsJoiner.buildQuery(query);

    assertTrue(queryParts.isEmpty());
  }

}