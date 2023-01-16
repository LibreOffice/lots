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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class ReplaceFunctionTest
{

  @Test
  public void replaceFunction() throws Exception
  {
    Function input = new StringLiteralFunction("abc");
    Pattern p = Pattern.compile("abc");
    Function replace = new StringLiteralFunction("def");
    Function f = new ReplaceFunction(input, p, replace);
    assertEquals(0, f.parameters().length);
    assertEquals("def", f.getResult(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new ReplaceFunction(new StringLiteralFunction(FunctionLibrary.ERROR), p, replace);
    assertEquals(FunctionLibrary.ERROR, f.getResult(null));

    f = new ReplaceFunction(input, p, new StringLiteralFunction(FunctionLibrary.ERROR));
    assertEquals(FunctionLibrary.ERROR, f.getResult(null));
  }

}
