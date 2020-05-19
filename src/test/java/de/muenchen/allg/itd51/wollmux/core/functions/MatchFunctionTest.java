package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class MatchFunctionTest
{

  @Test
  public void matchFunction() throws Exception
  {
    Function input = new StringLiteralFunction("abc");
    Pattern p = Pattern.compile("abc");
    Function f = new MatchFunction(input, p);
    assertEquals(0, f.parameters().length);
    assertEquals("true", f.getString(null));
    assertTrue(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new MatchFunction(new StringLiteralFunction(FunctionLibrary.ERROR), p);
    assertEquals(FunctionLibrary.ERROR, f.getString(null));
  }

}
