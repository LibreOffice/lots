package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

class DivideFunctionTest
{

  @Test
  public void divideFunction() throws Exception
  {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMinimumFractionDigits(1);

    Function divident = new StringLiteralFunction("10");
    Function divisor = new StringLiteralFunction("30");
    Function f = new DivideFunction(divident, divisor, 1, 3);
    assertEquals(0, f.parameters().length);
    assertEquals(nf.format(0.333), f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new DivideFunction(divident, null, 1, 2);
    assertEquals(nf.format(10.0), f.getString(null));

    f = new DivideFunction(divident, new StringLiteralFunction(FunctionLibrary.ERROR), 1, 2);
    assertEquals(FunctionLibrary.ERROR, f.getString(null));

    f = new DivideFunction(new StringLiteralFunction(FunctionLibrary.ERROR), divisor, 1, 2);
    assertEquals(FunctionLibrary.ERROR, f.getString(null));

    f = new DivideFunction(divident, new StringLiteralFunction("0"), 1, 2);
    assertEquals(FunctionLibrary.ERROR, f.getString(null));

    f = new DivideFunction(new StringLiteralFunction("0"), divisor, 0, 2);
    assertEquals("0", f.getString(null));
  }

}
