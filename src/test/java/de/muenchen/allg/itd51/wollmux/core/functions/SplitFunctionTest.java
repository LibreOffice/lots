package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class SplitFunctionTest
{

  @Test
  public void splitFunction() throws Exception
  {
    Function input = new StringLiteralFunction("abc");
    Pattern p = Pattern.compile("b");
    Function f = new SplitFunction(input, p, 0);
    assertEquals(0, f.parameters().length);
    assertEquals("a", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new SplitFunction(new StringLiteralFunction(FunctionLibrary.ERROR), p, 0);
    assertEquals(FunctionLibrary.ERROR, f.getString(null));

    f = new SplitFunction(input, p, -1);
    assertEquals(0, f.parameters().length);
    assertEquals("", f.getString(null));

    f = new SplitFunction(input, p, 10);
    assertEquals(0, f.parameters().length);
    assertEquals("", f.getString(null));
  }

}
