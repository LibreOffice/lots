package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class SearchStrategyTest
{

  @Test
  void testSearchStrategy() throws Exception
  {
    SearchStrategy strategy = SearchStrategy
        .parse(new ConfigThingy("",
            "Suchstrategie(ds (column \"${suchanfrage2}\" column2 \"${suchanfrage1}\") "
                + "ds (column \"${suchanfrage2}\" column2 \"${suchanfrage1}\"))"));
    List<Query> queries = strategy.getTemplate(2);
    assertEquals(2, queries.size());
    Query query = queries.get(0);
    assertEquals(2, query.numberOfQueryParts());
  }

}
