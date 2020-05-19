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

class BindFunctionTest
{

  @Test
  void testBindFunction() throws Exception
  {
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    HashMap<Object, Object> context = new HashMap<>();
    
    Function f = new BindFunction(new ValueFunction("test"), new ConfigThingy("", "SET(\"test\" \"value\")"), funcLib,
        dialogLib, context);
    assertEquals(0, f.parameters().length);
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());
    assertFalse(f.getBoolean(null));
    assertEquals("value", f.getString(null));

    f = new BindFunction(new ValueFunction("test"), new ConfigThingy("", ""), funcLib, dialogLib, context);
    Values.SimpleMap values = new Values.SimpleMap();
    values.put("test", "value2");
    assertEquals(1, f.parameters().length);
    assertFalse(f.getBoolean(values));
    assertEquals("value2", f.getString(values));

    f = new BindFunction(new ValueFunction("test"), new ConfigThingy("", "SET(\"test\" VALUE(\"test2\"))"), funcLib,
        dialogLib, context);
    values = new Values.SimpleMap();
    values.put("test2", FunctionLibrary.ERROR);
    assertFalse(f.getBoolean(values));
    assertEquals(FunctionLibrary.ERROR, f.getString(values));

    assertThrows(ConfigurationErrorException.class, () -> new BindFunction(new ValueFunction("test"),
        new ConfigThingy("", "SET(\"test\" \"value\") SET(\"test\" \"value\")"), funcLib, dialogLib, context));

    assertThrows(ConfigurationErrorException.class, () -> new BindFunction(new ValueFunction("test"),
        new ConfigThingy("", "SET(\"test\")"), funcLib, dialogLib, context));
  }

}
