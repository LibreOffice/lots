package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Collection;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

public class IsErrorFunction implements Function
{
  private Function func;

  private boolean objectCompare;

  /**
   * Falls objectCompare == true, wird == Function,ERROR getestet, ansonsten
   * equals(Function,ERROR).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public IsErrorFunction(boolean objectCompare, ConfigThingy conf,
      FunctionLibrary funcLib, DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    if (conf.count() != 1)
      throw new ConfigurationErrorException(L.m(
        "Funktion %1 muss genau einen Parameter haben", conf.getName()));

    this.objectCompare = objectCompare;
    func = FunctionFactory.parseChildren(conf, funcLib, dialogLib, context);
  }

  @Override
  public String[] parameters()
  {
    return func.parameters();
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    func.getFunctionDialogReferences(set);
  }

  @Override
  public String getString(Values parameters)
  {
    String str = func.getString(parameters);
    if (isError(str))
      return "true";
    else
      return "false";
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    String str = func.getString(parameters);
    return isError(str);
  }

  private boolean isError(String str)
  {
    if (objectCompare)
    {
      return Function.ERROR == str;
    }
    else
    {
      return Function.ERROR.equals(str);
    }
  }

}