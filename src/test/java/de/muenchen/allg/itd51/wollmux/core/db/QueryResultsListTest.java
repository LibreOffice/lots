package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDataset;

class QueryResultsListTest
{

  @Test
  void testQueryResultsList()
  {
    List<Dataset> dataList = List.of(new MockDataset());
    QueryResults res = new QueryResultsList(dataList.iterator(), 1);
    assertEquals(1, res.size());

    res = new QueryResultsList(Collections.emptyList());
    assertTrue(res.isEmpty());
    assertFalse(res.iterator().hasNext());
  }

}
