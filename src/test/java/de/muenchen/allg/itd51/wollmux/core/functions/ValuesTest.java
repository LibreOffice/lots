package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValuesTest
{

  @Test
  void none()
  {
    Values v = new Values.None();
    assertFalse(v.hasValue("test"));
    assertFalse(v.getBoolean("test"));
    assertEquals("", v.getString("test"));
  }

  @Test
  void simpleMap()
  {
    Values.SimpleMap v = new Values.SimpleMap();
    v.put("test", "a");
    assertTrue(v.hasValue("test"));
    assertFalse(v.getBoolean("test"));
    assertEquals("a", v.getString("test"));

    v.put("test", null);
    assertFalse(v.hasValue("test"));
    assertEquals("", v.getString("test"));
  }

}
