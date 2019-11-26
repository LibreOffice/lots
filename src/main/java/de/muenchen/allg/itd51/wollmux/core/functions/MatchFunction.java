package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Collection;
import java.util.regex.Pattern;

public class MatchFunction implements Function
{
  private Pattern pattern;

  private Function input;

  public MatchFunction(Function input, Pattern p)
  {
    pattern = p;
    this.input = input;
  }

  @Override
  public String getString(Values parameters)
  {
    String str = input.getString(parameters);
    if (str == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
    if (pattern.matcher(str).matches()) return "true";
    return "false";
  }

  @Override
  public String[] parameters()
  {
    return input.parameters();
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    input.getFunctionDialogReferences(set);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}