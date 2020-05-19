package de.muenchen.allg.itd51.wollmux.core.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

class ExternalFunctionTest
{

  @Test
  void testExternalFunction() throws Exception
  {
    ExternalFunction f = new ExternalFunction(new ConfigThingy("",
        "URL \"java:de.muenchen.allg.itd51.wollmux.core.functions.ExternalFunctionTest.extMethod\""));
    assertEquals(0, f.parameters().length);
    assertEquals("extMethod", f.invoke(new Values.None()));

    f = new ExternalFunction(new ConfigThingy("",
        "URL \"java:de.muenchen.allg.itd51.wollmux.core.functions.ExternalFunctionTest.extMethod2\" PARAMS(\"param\")"));
    Values.SimpleMap values = new Values.SimpleMap();
    values.put("param", "value");
    assertEquals(1, f.parameters().length);
    assertEquals("value", f.invoke(values));
    
    assertThrows(ConfigurationErrorException.class, () -> new ExternalFunction(new ConfigThingy("",
        "URL \"java:de.muenchen.allg.itd51.wollmux.core.functions.ExternalFunctionTest.extMethod4\"")));
    assertThrows(ConfigurationErrorException.class, () -> new ExternalFunction(
        new ConfigThingy("", "URL \"java:de.muenchen.allg.itd51.wollmux.core.functions.UnknownClass.extMethod4\"")));

    new ExternalFunction(new ConfigThingy("",
        "URL \"java:de.muenchen.allg.itd51.wollmux.core.functions.ExternalFunctionTest.extMethod3\""));
  }

  public static String extMethod()
  {
    return "extMethod";
  }

  public static String extMethod2(String param)
  {
    return param;
  }

  public static String extMethod3()
  {
    return "";
  }

  public static String extMethod3(String param)
  {
    return "";
  }

  private static String extMethod4(String param)
  {
    return "";
  }

}
