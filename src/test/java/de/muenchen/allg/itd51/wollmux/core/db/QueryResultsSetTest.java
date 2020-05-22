package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.core.db.mock.MockQueryResults;

class QueryResultsSetTest
{

  @Test
  void testQueryResultsSet()
  {
    Comparator<Dataset> comparator = new Comparator<Dataset>()
    {

      @Override
      public int compare(Dataset arg0, Dataset arg1)
      {
        try
        {
          return arg0.get("column").compareTo(arg1.get("column"));
        } catch (ColumnNotFoundException e)
        {
          return 0;
        }
      }
    };
    QueryResultsSet res = new QueryResultsSet(comparator);
    assertEquals(0, res.size());
    assertTrue(res.isEmpty());
    assertFalse(res.iterator().hasNext());

    res = new QueryResultsSet(comparator,
        new MockQueryResults(new MockDataset(), new MockDataset(), new MockDataset("ds", "column", "value2")));
    assertEquals(2, res.size());
  }

}
