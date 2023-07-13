/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.func.DivideFunction;
import org.libreoffice.lots.func.Function;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.func.StringLiteralFunction;

public class DivideFunctionTest
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
    assertEquals(nf.format(0.333), f.getResult(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new DivideFunction(divident, null, 1, 2);
    assertEquals(nf.format(10.0), f.getResult(null));

    f = new DivideFunction(divident, new StringLiteralFunction(FunctionLibrary.ERROR), 1, 2);
    assertEquals(FunctionLibrary.ERROR, f.getResult(null));

    f = new DivideFunction(new StringLiteralFunction(FunctionLibrary.ERROR), divisor, 1, 2);
    assertEquals(FunctionLibrary.ERROR, f.getResult(null));

    f = new DivideFunction(divident, new StringLiteralFunction("0"), 1, 2);
    assertEquals(FunctionLibrary.ERROR, f.getResult(null));

    f = new DivideFunction(new StringLiteralFunction("0"), divisor, 0, 2);
    assertEquals("0", f.getResult(null));
  }

}
