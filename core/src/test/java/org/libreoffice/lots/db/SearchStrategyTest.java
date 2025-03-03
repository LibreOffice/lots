/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.db.Query;
import org.libreoffice.lots.db.SearchStrategy;

public class SearchStrategyTest
{

  @Test
  public void testSearchStrategy() throws Exception
  {
    SearchStrategy strategy = SearchStrategy
        .parse(new ConfigThingy("",
            "SearchStrategy(ds (column \"${suchanfrage2}\" column2 \"${suchanfrage1}\") "
                + "ds (column \"${suchanfrage2}\" column2 \"${suchanfrage1}\"))"));
    List<Query> queries = strategy.getTemplate(2);
    assertEquals(2, queries.size());
    Query query = queries.get(0);
    assertEquals(2, query.numberOfQueryParts());
  }

}
