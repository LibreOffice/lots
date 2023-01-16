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
package de.muenchen.allg.itd51.wollmux.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.func.print.PrintException;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;

public class PrintFunctionLibraryTest
{
  private static final String functionString = "TestFunction(EXTERN(URL \"java:de.muenchen.allg.itd51.wollmux.print.PrintFunctionLibraryTest.testDefaultPrintFunctions\") ORDER \"50\")";
  private static final String confString = "Druckfunktionen(" + functionString + ")";
  private static PrintFunction testFunction = new PrintFunction("test", 300)
  {

    @Override
    public void print(XPrintModel printModel) throws PrintException
    {
      // do nothing
    }
  };

  @Test
  public void testDefaultPrintFunctions()
  {
    PrintFunctionLibrary library = new PrintFunctionLibrary();
    PrintFunction.addPrintFunctions(library);
    assertEquals(13, library.getFunctionNames().size(), "Not enough print functions");
  }

  @Test
  public void testAddPrintFunction()
  {
    PrintFunctionLibrary library = new PrintFunctionLibrary();
    assertThrows(NullPointerException.class, () -> library.add("test", null), "No function was given");
    assertThrows(NullPointerException.class, () -> library.add(null, testFunction), "No name was given");
  }

  @Test
  public void testFunctionNames()
  {
    PrintFunctionLibrary base = new PrintFunctionLibrary();
    base.add("baseTest", testFunction);
    PrintFunctionLibrary library = new PrintFunctionLibrary(base);
    library.add("libraryTest", testFunction);
    Set<String> names = library.getFunctionNames();
    assertTrue(names.contains("baseTest"), "Missing base function name");
    assertTrue(names.contains("libraryTest"), "Missing library function name");
    assertNull(library.get("test"), "Contains undefined method");
    assertNotNull(library.get("baseTest"), "Missing base function");
    assertNotNull(library.get("libraryTest"), "Missing library function");
  }

  @Test
  public void testParsePrintFunctions() throws Exception
  {
    PrintFunctionLibrary library = PrintFunctionLibrary.parsePrintFunctions(new ConfigThingy("test", confString));
    assertNotNull(library.get("TestFunction"), "Missing function by parser");
  }

}
