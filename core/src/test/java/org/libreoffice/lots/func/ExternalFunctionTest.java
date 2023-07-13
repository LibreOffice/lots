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

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.func.ExternalFunction;
import org.libreoffice.lots.func.Values;

public class ExternalFunctionTest
{

  @Test
  public void testExternalFunction() throws Exception
  {
    ExternalFunction f = new ExternalFunction(new ConfigThingy("",
        "URL \"java:org.libreoffice.lots.func.ExternalFunctionTest.extMethod\""));
    assertEquals(0, f.parameters().length);
    assertEquals("extMethod", f.invoke(new Values.None()));

    f = new ExternalFunction(new ConfigThingy("",
        "URL \"java:org.libreoffice.lots.func.ExternalFunctionTest.extMethod2\" PARAMS(\"param\")"));
    Values.SimpleMap values = new Values.SimpleMap();
    values.put("param", "value");
    assertEquals(1, f.parameters().length);
    assertEquals("value", f.invoke(values));

    assertThrows(ConfigurationErrorException.class, () -> new ExternalFunction(new ConfigThingy("",
        "URL \"java:org.libreoffice.lots.func.ExternalFunctionTest.extMethod4\"")));
    assertThrows(ConfigurationErrorException.class, () -> new ExternalFunction(
        new ConfigThingy("", "URL \"java:org.libreoffice.lots.func.UnknownClass.extMethod4\"")));

    new ExternalFunction(new ConfigThingy("",
        "URL \"java:org.libreoffice.lots.func.ExternalFunctionTest.extMethod3\""));
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
