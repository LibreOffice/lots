/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;

public class ExternalFunctionFunctionTest
{

  @Test
  public void testExternalFunctionFunction() throws Exception
  {
    Function f = new ExternalFunctionFunction(new ConfigThingy("",
        "URL \"java:de.muenchen.allg.itd51.wollmux.func.ExternalFunctionFunctionTest.extMethod\""));
    assertEquals(0, f.parameters().length);
    assertEquals("extMethod", f.getString(null));
    assertFalse(f.getBoolean(null));
    Collection<String> dialogFunctions = new ArrayList<>();
    f.getFunctionDialogReferences(dialogFunctions);
    assertTrue(dialogFunctions.isEmpty());

    f = new ExternalFunctionFunction(new ConfigThingy("",
        "URL \"java:de.muenchen.allg.itd51.wollmux.func.ExternalFunctionFunctionTest.extMethod2\""));
    assertEquals(FunctionLibrary.ERROR, f.getString(null));
  }

  public static String extMethod()
  {
    return "extMethod";
  }

  public static String extMethod2()
  {
    return null;
  }
}
