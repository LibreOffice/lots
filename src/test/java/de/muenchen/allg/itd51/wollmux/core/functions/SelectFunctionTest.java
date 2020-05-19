package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class SelectFunctionTest
{

  @Test
  public void selectFunction() throws Exception
  {
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    HashMap<Object, Object> context = new HashMap<>();

    Function f = new SelectFunction(
        new ConfigThingy("SELECT", "IF(\"true\" THEN \"1\") IF(\"true\" THEN \"2\") ELSE \"3\" ONERROR \"error\""),
        funcLib, dialogLib, context);
    assertEquals(0, f.parameters().length);
    assertEquals("1", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new SelectFunction(
        new ConfigThingy("SELECT", "IF(\"false\" THEN \"1\") IF(\"true\" THEN \"2\") ELSE \"3\" ONERROR \"error\""),
        funcLib, dialogLib, context);
    assertEquals("2", f.getString(null));

    f = new SelectFunction(
        new ConfigThingy("SELECT", "IF(\"false\" THEN \"1\") IF(\"false\" THEN \"2\") ELSE \"3\" ONERROR \"error\""),
        funcLib, dialogLib, context);
    assertEquals("3", f.getString(null));

    f = new SelectFunction(List.of(new StringLiteralFunction(FunctionLibrary.ERROR)));
    assertEquals(FunctionLibrary.ERROR, f.getString(null));

    SelectFunction sf = new SelectFunction(List.of(new StringLiteralFunction(FunctionLibrary.ERROR)));
    sf.handleParam(new ConfigThingy("ONERROR", "\"error\""), funcLib, dialogLib, context);
    assertEquals("error", sf.getString(null));

    f = new SelectFunction(Collections.emptyList());
    assertEquals(FunctionLibrary.ERROR, f.getString(null));
  }

}
