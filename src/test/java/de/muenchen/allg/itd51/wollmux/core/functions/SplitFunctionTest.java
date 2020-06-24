/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class SplitFunctionTest
{

  @Test
  public void splitFunction() throws Exception
  {
    Function input = new StringLiteralFunction("abc");
    Pattern p = Pattern.compile("b");
    Function f = new SplitFunction(input, p, 0);
    assertEquals(0, f.parameters().length);
    assertEquals("a", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new SplitFunction(new StringLiteralFunction(FunctionLibrary.ERROR), p, 0);
    assertEquals(FunctionLibrary.ERROR, f.getString(null));

    f = new SplitFunction(input, p, -1);
    assertEquals(0, f.parameters().length);
    assertEquals("", f.getString(null));

    f = new SplitFunction(input, p, 10);
    assertEquals(0, f.parameters().length);
    assertEquals("", f.getString(null));
  }

}
