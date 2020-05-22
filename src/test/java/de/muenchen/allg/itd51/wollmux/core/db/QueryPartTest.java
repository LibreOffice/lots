package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class QueryPartTest
{

  @Test
  void testQueryPart()
  {
    QueryPart part = new QueryPart("column", "search");
    assertEquals("column", part.getColumnName());
    assertEquals("search", part.getSearchString());
    assertEquals("column=search", part.toString());

    assertThrows(NullPointerException.class, () -> new QueryPart(null, "search"));

    part = new QueryPart("column", null);
    assertEquals("", part.getSearchString());
  }

}
