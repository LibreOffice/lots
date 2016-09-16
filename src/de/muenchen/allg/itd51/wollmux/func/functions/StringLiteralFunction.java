package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Collection;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

public class StringLiteralFunction implements Function
{
  private String literal;

  private boolean bool;

  @Override
  public String[] parameters()
  {
    return noParams;
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