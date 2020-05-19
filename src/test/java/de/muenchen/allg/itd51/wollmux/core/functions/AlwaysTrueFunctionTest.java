package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

class AlwaysTrueFunctionTest
{
  @Test
  public void alwaysTrueFunction()
  {
    Function f = new AlwaysTrueFunction();
    assertEquals(0, f.parameters().length);
    assertEquals("true", f.getString(null));
    assertTrue(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());
  }

}
