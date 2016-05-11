package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Collection;

import de.muenchen.allg.itd51.wollmux.WollMuxClassLoader;
import de.muenchen.allg.itd51.wollmux.core.functions.ExternalFunction;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

public class ExternalFunctionFunction implements Function
{
  private ExternalFunction func;

  public ExternalFunctionFunction(ConfigThingy conf)
      throws ConfigurationErrorException
  {
    func = new ExternalFunction(conf, WollMuxClassLoader.getClassLoader());
  }

  @Override
  public String[] parameters()
  {
    return func.parameters();
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {}

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
      Logger.error(e);
      return Function.ERROR;
    }
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}