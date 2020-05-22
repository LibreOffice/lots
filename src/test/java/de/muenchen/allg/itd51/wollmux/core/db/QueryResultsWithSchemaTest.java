package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockQueryResults;

class QueryResultsWithSchemaTest
{

  @Test
  void test()
  {
    QueryResultsWithSchema results = new QueryResultsWithSchema();
    assertTrue(results.isEmpty());

    results = new QueryResultsWithSchema(new MockQueryResults(), List.of("column"));
    assertEquals(1, results.size());
    assertTrue(results.getSchema().contains("column"));
    assertTrue(results.iterator().hasNext());
  }

}
