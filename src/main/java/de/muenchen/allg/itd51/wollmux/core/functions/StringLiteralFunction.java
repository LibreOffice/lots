package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;

public class StringLiteralFunction implements Function
{
  private String literal;

  private boolean bool;

  @Override
  public String[] parameters()
  {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {}

  public StringLiteralFunction(String str)
  {
    literal = str;
    bool = literal.equalsIgnoreCase("true");
  }

  @Override
  public String getString(Values parameters)
  {
    return literal;
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return bool;
  }
}