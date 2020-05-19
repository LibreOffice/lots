package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

class ValueFunctionTest
{

  @Test
  public void valueFunction() throws Exception
  {
    Function f = new ValueFunction("test");
    assertEquals(1, f.parameters().length);
    Values.SimpleMap v = new Values.SimpleMap();
    v.put("test", "a");
    assertEquals("a", f.getString(v));
    assertFalse(f.getBoolean(v));
    assertEquals(FunctionLibrary.ERROR, f.getString(new Values.None()));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());
  }

}
