package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

public class AlwaysTrueFunction implements Function
{
  @Override
  public String[] parameters()
  {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {}

  @Override
  public String getString(Values parameters)
  {
    return "true";
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return true;
  }
}