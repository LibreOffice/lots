package de.muenchen.allg.itd151.wollmux.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.func.StandardPrint;
import de.muenchen.allg.itd51.wollmux.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.print.PrintFunctionLibrary;

public class PrintFunctionLibraryTest
{
  private static final String functionString = "TestFunction(EXTERN(URL \"java:de.muenchen.allg.itd151.wollmux.print.PrintFunctionLibraryTest.testDefaultPrintFunctions\") ORDER \"50\")";
  private static final String confString = "Druckfunktionen(" + functionString + ")";
  private static PrintFunction testFunction;

  @BeforeAll
  public static void setUp() throws Exception
  {
    testFunction = new PrintFunction(new ConfigThingy("test", functionString), "test", 300);
  }

  @Test
  public void testDefaultPrintFunctions()
  {
    PrintFunctionLibrary library = new PrintFunctionLibrary();
    StandardPrint.addInternalDefaultPrintFunctions(library);
    assertEquals(9, library.getFunctionNames().size(), "Not enough print functions");
  }

  @Test
  public void testAddPrintFunction()
  {
    PrintFunctionLibrary library = new PrintFunctionLibrary();
    assertThrows(NullPointerException.class, () -> library.add("test", null),
        "No function was given");
    assertThrows(NullPointerException.class,
        () -> library.add(null, testFunction),
        "No name was given");
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
    assertTrue(library.get("test") == null, "Contains undefined method");
    assertTrue(library.get("baseTest") != null, "Missing base function");
    assertTrue(library.get("libraryTest") != null, "Missing library function");
  }

  @Test
  public void testParsePrintFunctions() throws Exception
  {
    PrintFunctionLibrary library = PrintFunctionLibrary
        .parsePrintFunctions(new ConfigThingy("test", confString));
    assertTrue(library.get("TestFunction") != null, "Missing function by parser");
  }

}
