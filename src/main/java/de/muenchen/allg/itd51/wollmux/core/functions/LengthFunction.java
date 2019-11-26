package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class LengthFunction extends CatFunction
{
  public LengthFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  public String getString(Values parameters)
  {
    String res = super.getString(parameters);
    if (res == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
    return "" + res.length();
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return false;
  }
}