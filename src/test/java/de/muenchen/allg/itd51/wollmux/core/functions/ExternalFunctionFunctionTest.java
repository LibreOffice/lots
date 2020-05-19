package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class ExternalFunctionFunctionTest
{

  @Test
  public void testExternalFunctionFunction() throws Exception
  {
    Function f = new ExternalFunctionFunction(new ConfigThingy("",
        "URL \"java:de.muenchen.allg.itd51.wollmux.core.functions.ExternalFunctionFunctionTest.extMethod\""));
    assertEquals(0, f.parameters().length);
    assertEquals("extMethod", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new ExternalFunctionFunction(new ConfigThingy("",
        "URL \"java:de.muenchen.allg.itd51.wollmux.core.functions.ExternalFunctionFunctionTest.extMethod2\""));
    assertEquals(FunctionLibrary.ERROR, f.getString(null));
  }

  public static String extMethod()
  {
    return "extMethod";
  }

  public static String extMethod2()
  {
    return null;
  }
}
