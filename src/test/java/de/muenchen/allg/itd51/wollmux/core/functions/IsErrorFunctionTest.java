package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

class IsErrorFunctionTest
{

  @Test
  void isErrorFunction() throws Exception
  {
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    HashMap<Object, Object> context = new HashMap<>();

    Function f = new IsErrorFunction(true, new ConfigThingy("", "\"test\""), funcLib, dialogLib, context);
    assertEquals(0, f.parameters().length);
    assertEquals("false", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new IsErrorFunction(false, new ConfigThingy("", String.format("\"%s\"", FunctionLibrary.ERROR)), funcLib,
        dialogLib, context);
    assertEquals(0, f.parameters().length);
    assertEquals("true", f.getString(null));
    assertTrue(f.getBoolean(null));

    assertThrows(ConfigurationErrorException.class,
        () -> new IsErrorFunction(false, new ConfigThingy("", "\"test\" \"test\""), funcLib, dialogLib, context));
  }

}
