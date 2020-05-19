package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ReplaceFunctionTest
{

  @Test
  public void replaceFunction() throws Exception
  {
    Function input = new StringLiteralFunction("abc");
    Pattern p = Pattern.compile("abc");
    Function replace = new StringLiteralFunction("def");
    Function f = new ReplaceFunction(input, p, replace);
    assertEquals(0, f.parameters().length);
    assertEquals("def", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new ReplaceFunction(new StringLiteralFunction(FunctionLibrary.ERROR), p, replace);
    assertEquals(FunctionLibrary.ERROR, f.getString(null));

    f = new ReplaceFunction(input, p, new StringLiteralFunction(FunctionLibrary.ERROR));
    assertEquals(FunctionLibrary.ERROR, f.getString(null));
  }

}
