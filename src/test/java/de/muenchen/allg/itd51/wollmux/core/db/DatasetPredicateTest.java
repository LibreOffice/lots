package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class DatasetPredicateTest
{
  @Test
  public void makePredicateValidEntry() throws Exception
  {
    List<QueryPart> query = new ArrayList<>();
    query.add(new QueryPart("Vorname", "*Sheldon"));
    query.add(new QueryPart("Vorname", "Sheldon")); // Valid
    query.add(new QueryPart("Vorname", "Sheldon*"));

    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);

    Map<String, String> testMap = new HashMap<>();
    testMap.put("Vorname", "Sheldon");

    Dataset ds = new SimpleDataset("Test", testMap);

    boolean result = pred.test(ds);

    assertTrue(result, "Given dataset does not match with a given QueryPart:");
  }

  @Test
  public void makePredicateInValidEntry() throws Exception
  {
    List<QueryPart> query = new ArrayList<>();

    query.add(new QueryPart("Vorname", "*Sheldon"));
    query.add(new QueryPart("Vorname", "Sheldon*"));

    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);

    Map<String, String> testMap = new HashMap<>();
    testMap.put("Vorname", "Sheldon");

    Dataset ds = new SimpleDataset("Test", testMap);

    boolean result = pred.test(ds);

    assertTrue(result, "Given dataset does not match with a given QueryPart:");
  }

  @Test
  public void makePredicateCheckCount() throws Exception
  {
    List<QueryPart> query = new ArrayList<>();

    query.add(new QueryPart("Vorname", "*Sheldon"));
    query.add(new QueryPart("Vorname", "Sheldon*"));
    query.add(new QueryPart("Vorname", "*Sheldon*"));
    query.add(new QueryPart("Vorname", "*Sheldon**"));
    query.add(new QueryPart("Vorname", "**Sheldon*"));
    query.add(new QueryPart("Vorname", "**Sheldon**"));

    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);

    Map<String, String> testMap = new HashMap<>();
    testMap.put("Vorname", "Sheldon");

    Map<String, String> testMap1 = new HashMap<>();
    testMap1.put("Vorname", "Sheldon*");

    List<Dataset> datasetList = new ArrayList<>();

    Dataset ds = new SimpleDataset("Test", testMap);
    Dataset ds1 = new SimpleDataset("Test", testMap1);

    datasetList.add(ds);
    datasetList.add(ds1);

    int count = 0;
    for (Dataset dataset : datasetList)
    {
      boolean result = pred.test(dataset);

      if (result)
        count++;
    }

    assertEquals(1, count, "Given datasets have a wrong match count with given QueryParts:");
  }

  @Test
  public void matchAll()
  {
    Predicate<Dataset> pred = DatasetPredicate.matchAll;

    Map<String, String> testMap = new HashMap<>();
    testMap.put("Vorname", "Sheldon");

    Map<String, String> testMap1 = new HashMap<>();
    testMap1.put("Vorname", "Sheldon*");

    Map<String, String> testMap2 = new HashMap<>();
    testMap1.put("Vorname", "**Sheldon**");

    List<Dataset> datasetList = new ArrayList<>();

    Dataset ds = new SimpleDataset("Test", testMap);
    Dataset ds1 = new SimpleDataset("Test", testMap1);
    Dataset ds2 = new SimpleDataset("Test", testMap2);

    datasetList.add(ds);
    datasetList.add(ds1);
    datasetList.add(ds2);

    int count = 0;
    for (Dataset dataset : datasetList)
    {
      boolean result = pred.test(dataset);

      if (result)
        count++;
    }

    assertEquals(3, count, "Given datasets have a wrong match count with given QueryParts:");
  }

}
