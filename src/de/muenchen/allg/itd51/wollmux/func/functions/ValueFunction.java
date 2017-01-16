package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Collection;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

public class ValueFunction implements Function
{
  String[] params;

  public ValueFunction(String valueName)
  {
    params = new String[] { valueName };
  }

  @Override
  public String[] parameters()
  {
    return params;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {}

  @Override
  public String getString(Values parameters)
  {
    if (!parameters.hasValue(params[0])) return Function.ERROR;
    return parameters.getString(params[0]);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}