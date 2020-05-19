package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

class NumberCompareFunctionTest
{

  @Test
  public void numberCompareFunction() throws Exception
  {
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    Map<Object, Object> context = new HashMap<>();

    String zeroPointOne = NumberFormat.getInstance().format(0.1);
    String onePointOne = NumberFormat.getInstance().format(1.1);
    String zeroPointNine = NumberFormat.getInstance().format(0.9);
    NumberCompareFunction ncf = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        new ConfigThingy("NUMCMP", "\"1\" \"" + onePointOne + "\" \"" + zeroPointNine + "\""), funcLib, dialogLib,
        context);
    assertThrows(ConfigurationErrorException.class,
        () -> ncf.handleParam(new ConfigThingy("MARGIN", "\"" + zeroPointOne + "\" \"1\""), funcLib, dialogLib,
            context));
    ncf.handleParam(new ConfigThingy("MARGIN", "\"" + zeroPointOne + "\""), funcLib, dialogLib, context);
    assertEquals(0, ncf.parameters().length);
    assertEquals("true", ncf.getString(null));
    assertTrue(ncf.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    ncf.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());
    ncf.handleParam(new ConfigThingy("MARGIN", "\"test\""), funcLib, dialogLib, context);
    assertEquals(FunctionLibrary.ERROR, ncf.getString(null));

    NumberCompareFunction f = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        new ConfigThingy("NUMCMP", "\"1\" \"" + zeroPointNine + "\" \"2\""), funcLib, dialogLib, context);
    f.handleParam(new ConfigThingy("MARGIN", "\"" + zeroPointOne + "\""), funcLib, dialogLib, context);
    assertEquals("-1", f.getString(null));

    f = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        new ConfigThingy("NUMCMP", "\"2\" \"1\" \"2\""), funcLib, dialogLib, context);
    dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());
    assertEquals("1", f.getString(null));

    f = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        new ConfigThingy("NUMCMP", "\"2\" \"1\" \"3\""), funcLib, dialogLib, context);
    assertEquals("0", f.getString(null));

    f = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, "result",
        new ConfigThingy("NUMCMP", "\"2\" \"2\" \"2\""), funcLib, dialogLib, context);
    assertEquals("result", f.getString(null));

    f = new NumberCompareFunction(1, Integer.MAX_VALUE, null, new ConfigThingy("NUMCMP", "\"2\" \"1\" \"0\""), funcLib,
        dialogLib, context);
    assertEquals("false", f.getString(null));

    f = new NumberCompareFunction(Integer.MAX_VALUE, 1, null, new ConfigThingy("NUMCMP", "\"2\" \"1\" \"0\""), funcLib,
        dialogLib, context);
    assertEquals("false", f.getString(null));

    assertThrows(ConfigurationErrorException.class, () -> new NumberCompareFunction(Integer.MAX_VALUE,
        Integer.MAX_VALUE, null, new ConfigThingy("NUMCMP", "\"1\""), funcLib, dialogLib, context));
  }

}
