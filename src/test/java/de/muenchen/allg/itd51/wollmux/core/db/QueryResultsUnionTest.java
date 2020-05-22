package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockQueryResults;

class QueryResultsUnionTest
{

  @Test
  void testQueryResultsUnion()
  {
    QueryResults results1 = new MockQueryResults();
    QueryResults results2 = new MockQueryResults();
    QueryResults res = new QueryResultsUnion(results1, results2);
    assertEquals(2, res.size());
    assertFalse(res.isEmpty());
    Iterator<Dataset> iter = res.iterator();
    assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    assertTrue(iter.hasNext());
    iter.next();
    assertTrue(iter.hasNext());
    iter.next();
    assertFalse(iter.hasNext());

    res = new QueryResultsUnion(new MockQueryResults(new Dataset[] {}), new MockQueryResults(new Dataset[] {}));
    assertTrue(res.isEmpty());
    res = new QueryResultsUnion(new MockQueryResults(new Dataset[] {}), new MockQueryResults());
    assertFalse(res.isEmpty());
  }

}
