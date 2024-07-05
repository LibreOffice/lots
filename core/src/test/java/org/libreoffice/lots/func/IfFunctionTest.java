/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.func.Function;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.func.IfFunction;
import org.libreoffice.lots.func.StringLiteralFunction;

public class IfFunctionTest
{

  @Test
  public void ifFunction() throws Exception
  {
    Function cond1 = new StringLiteralFunction("true");
    Function cond2 = new StringLiteralFunction("false");
    Function then = new StringLiteralFunction("then");
    Function el = new StringLiteralFunction("else");
    Function f = new IfFunction(cond1, then, el);
    assertEquals(0, f.parameters().length);
    assertEquals("then", f.getResult(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new IfFunction(cond2, then, el);
    assertEquals("else", f.getResult(null));
    assertFalse(f.getBoolean(null));

    f = new IfFunction(new StringLiteralFunction(FunctionLibrary.ERROR), then, el);
    assertEquals(FunctionLibrary.ERROR, f.getResult(null));
    assertFalse(f.getBoolean(null));
  }

}
