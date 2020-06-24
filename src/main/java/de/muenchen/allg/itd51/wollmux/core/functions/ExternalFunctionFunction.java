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

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.ConfClassLoader;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;

public class ExternalFunctionFunction implements Function
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ExternalFunctionFunction.class);

  private ExternalFunction func;

  public ExternalFunctionFunction(ConfigThingy conf)
  {
    func = new ExternalFunction(conf, ConfClassLoader.getClassLoader());
  }

  @Override
  public String[] parameters()
  {
    return func.parameters();
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    // Externe Funtkionen haben keine Dialoge.
  }

  @Override
  public String getString(Values parameters)
  {
    try
    {
      Object result = func.invoke(parameters);
      if (result == null)
        throw new Exception(
          L.m("Unbekannter Fehler beim Ausführen einer externen Funktion"));
      return result.toString();
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
      return FunctionLibrary.ERROR;
    }
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}