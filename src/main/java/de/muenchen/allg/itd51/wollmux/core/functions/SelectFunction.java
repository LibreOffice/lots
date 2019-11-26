package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class SelectFunction extends MultiFunction
{
  private Function onErrorFunction;

  public SelectFunction(Collection<Function> subFunction)
  {
    super(subFunction);
  }

  public SelectFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.getName().equals("ONERROR"))
    {
      onErrorFunction = new CatFunction(conf, funcLib, dialogLib, context);
      return true;
    }
    return false;
  }

  @Override
  protected String[] getAdditionalParams()
  {
    if (onErrorFunction != null)
      return onErrorFunction.parameters();
    else
      return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    super.getFunctionDialogReferences(set);
    if (onErrorFunction != null) onErrorFunction.getFunctionDialogReferences(set);
  }

  @Override
  public String getString(Values parameters)
  {
    Iterator<Function> iter = subFunction.iterator();
    String result = FunctionLibrary.ERROR;
    while (iter.hasNext())
    {
      Function func = iter.next();
      String str = func.getString(parameters);
      if (str != FunctionLibrary.ERROR)
      {
        result = str;
        if (str.length() > 0) break;
      }
      else if (onErrorFunction != null)
      {
        return onErrorFunction.getString(parameters);
      }
    }
    return result;
  }
}