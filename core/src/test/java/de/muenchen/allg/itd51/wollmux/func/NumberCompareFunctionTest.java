/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;

public class NumberCompareFunctionTest
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
    assertEquals("true", ncf.getResult(null));
    assertTrue(ncf.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    ncf.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());
    ncf.handleParam(new ConfigThingy("MARGIN", "\"test\""), funcLib, dialogLib, context);
    assertEquals(FunctionLibrary.ERROR, ncf.getResult(null));

    NumberCompareFunction f = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        new ConfigThingy("NUMCMP", "\"1\" \"" + zeroPointNine + "\" \"2\""), funcLib, dialogLib, context);
    f.handleParam(new ConfigThingy("MARGIN", "\"" + zeroPointOne + "\""), funcLib, dialogLib, context);
    assertEquals("-1", f.getResult(null));

    f = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        new ConfigThingy("NUMCMP", "\"2\" \"1\" \"2\""), funcLib, dialogLib, context);
    dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());
    assertEquals("1", f.getResult(null));

    f = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        new ConfigThingy("NUMCMP", "\"2\" \"1\" \"3\""), funcLib, dialogLib, context);
    assertEquals("0", f.getResult(null));

    f = new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, "result",
        new ConfigThingy("NUMCMP", "\"2\" \"2\" \"2\""), funcLib, dialogLib, context);
    assertEquals("result", f.getResult(null));

    f = new NumberCompareFunction(1, Integer.MAX_VALUE, null, new ConfigThingy("NUMCMP", "\"2\" \"1\" \"0\""), funcLib,
        dialogLib, context);
    assertEquals("false", f.getResult(null));

    f = new NumberCompareFunction(Integer.MAX_VALUE, 1, null, new ConfigThingy("NUMCMP", "\"2\" \"1\" \"0\""), funcLib,
        dialogLib, context);
    assertEquals("false", f.getResult(null));

    assertThrows(ConfigurationErrorException.class, () -> new NumberCompareFunction(Integer.MAX_VALUE,
        Integer.MAX_VALUE, null, new ConfigThingy("NUMCMP", "\"1\""), funcLib, dialogLib, context));
  }

}
