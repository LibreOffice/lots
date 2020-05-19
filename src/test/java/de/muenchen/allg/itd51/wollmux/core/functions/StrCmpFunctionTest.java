package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

class StrCmpFunctionTest
{

  @Test
  public void strCmpFunction() throws Exception
  {
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    HashMap<Object, Object> context = new HashMap<>();

    Function f = new StrCmpFunction(new ConfigThingy("STRCMP", "\"a\" \"a\" \"a\""), funcLib, dialogLib, context);
    assertEquals(0, f.parameters().length);
    assertEquals("true", f.getString(null));
    assertTrue(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new StrCmpFunction(new ConfigThingy("STRCMP", "\"a\" \"a\" \"z\""), funcLib, dialogLib, context);
    assertEquals("-1", f.getString(null));

    f = new StrCmpFunction(new ConfigThingy("STRCMP", "\"z\" \"a\" \"z\""), funcLib, dialogLib, context);
    assertEquals("1", f.getString(null));

    f = new StrCmpFunction(new ConfigThingy("STRCMP", "\"m\" \"a\" \"z\""), funcLib, dialogLib, context);
    assertEquals("0", f.getString(null));

    assertThrows(ConfigurationErrorException.class,
        () -> new StrCmpFunction(new ConfigThingy("STRCMP", "\"a\""), funcLib, dialogLib, context));
  }

}
