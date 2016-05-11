package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

public class StrCmpFunction extends MultiFunction
{
  public StrCmpFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    super(conf, funcLib, dialogLib, context);
    if (subFunction.size() < 2)
      throw new ConfigurationErrorException(L.m(
        "Funktion %1 erfordert mindestens 2 Parameter", conf.getName()));
  }

  @Override
  public String getString(Values parameters)
  {
    Iterator<Function> iter = subFunction.iterator();
    Function func = iter.next();
    String compare = func.getString(parameters);
    if (compare == Function.ERROR) return Function.ERROR;
    int prevCompare = 0;
    while (iter.hasNext())
    {
      func = iter.next();
      String str = func.getString(parameters);
      if (str == Function.ERROR) return Function.ERROR;
      int res = Integer.signum(compare.compareTo(str));
      if (res * prevCompare < 0) return "0";
      prevCompare += res;
    }

    switch (Integer.signum(prevCompare))
    {
      case -1:
        return "-1";
      case 1:
        return "1";
      default:
        return "true";
    }
  }
}