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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.dialog.DialogLibrary;
import org.libreoffice.lots.func.Function;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.func.StrCmpFunction;

public class StrCmpFunctionTest
{

  @Test
  public void strCmpFunction() throws Exception
  {
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    HashMap<Object, Object> context = new HashMap<>();

    Function f = new StrCmpFunction(new ConfigThingy("STRCMP", "\"a\" \"a\" \"a\""), funcLib, dialogLib, context);
    assertEquals(0, f.parameters().length);
    assertEquals("true", f.getResult(null));
    assertTrue(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new StrCmpFunction(new ConfigThingy("STRCMP", "\"a\" \"a\" \"z\""), funcLib, dialogLib, context);
    assertEquals("-1", f.getResult(null));

    f = new StrCmpFunction(new ConfigThingy("STRCMP", "\"z\" \"a\" \"z\""), funcLib, dialogLib, context);
    assertEquals("1", f.getResult(null));

    f = new StrCmpFunction(new ConfigThingy("STRCMP", "\"m\" \"a\" \"z\""), funcLib, dialogLib, context);
    assertEquals("0", f.getResult(null));

    assertThrows(ConfigurationErrorException.class,
        () -> new StrCmpFunction(new ConfigThingy("STRCMP", "\"a\""), funcLib, dialogLib, context));
  }

}
