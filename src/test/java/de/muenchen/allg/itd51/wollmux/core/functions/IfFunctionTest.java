package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

class IfFunctionTest
{

  @Test
  public void ifFunction() throws Exception
  {
    Function cond1 = new StringLiteralFunction("true");
    Function cond2 = new StringLiteralFunction("false");
    Function then = new StringLiteralFunction("then");
    Function el = new StringLiteralFunction("else");
    Function f = new IfFunction(cond1, then, el);
    assertEquals(0, f.parameters().length);
    assertEquals("then", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new IfFunction(cond2, then, el);
    assertEquals("else", f.getString(null));
    assertFalse(f.getBoolean(null));

    f = new IfFunction(new StringLiteralFunction(FunctionLibrary.ERROR), then, el);
    assertEquals(FunctionLibrary.ERROR, f.getString(null));
    assertFalse(f.getBoolean(null));
  }

}
