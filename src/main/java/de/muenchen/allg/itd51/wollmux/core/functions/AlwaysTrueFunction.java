package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;

public class AlwaysTrueFunction implements Function
{
  @Override
  public String[] parameters()
  {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    // Hat keine Referenzen auf Dialoge.
  }

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