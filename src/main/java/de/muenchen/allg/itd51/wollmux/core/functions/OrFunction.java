package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class OrFunction extends MultiFunction
{
  public OrFunction(Collection<Function> subFunction)
  {
    super(subFunction);
  }

  public OrFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  public String getString(Values parameters)
  {
    Iterator<Function> iter = subFunction.iterator();
    while (iter.hasNext())
    {
      Function func = iter.next();
      String str = func.getString(parameters);
      if (str == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
      if (str.equalsIgnoreCase("true")) return "true";
    }
    return "false";
  }
}