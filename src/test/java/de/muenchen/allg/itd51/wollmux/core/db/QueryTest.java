package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class QueryTest
{

  @Test
  void testQuery()
  {
    Query query = new Query("ds", List.of(new QueryPart("column", "search")));
    assertEquals("ds", query.getDatasourceName());
    assertEquals(1, query.numberOfQueryParts());
    assertTrue(query.iterator().hasNext());
    assertEquals(1, query.getQueryParts().size());
  }

}
