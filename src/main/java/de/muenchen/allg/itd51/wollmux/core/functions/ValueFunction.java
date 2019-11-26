package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Collection;

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
  {
    // Value Function hat keine Referenzen auf Dialoge.
  }

  @Override
  public String getString(Values parameters)
  {
    if (!parameters.hasValue(params[0])) return FunctionLibrary.ERROR;
    return parameters.getString(params[0]);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}