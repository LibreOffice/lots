package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class LengthFunctionTest
{

  @Test
  public void lengthFunction() throws Exception
  {
    Function f = new LengthFunction(new ConfigThingy("LENGTH", "\"aaaaa\""), new FunctionLibrary(), new DialogLibrary(),
        new HashMap<>());
    assertEquals(0, f.parameters().length);
    assertEquals("5", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());
  }

}
