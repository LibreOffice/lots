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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.db.mock.MockDataset;
import de.muenchen.allg.itd51.wollmux.db.mock.MockQueryResults;

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
