package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Collection;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

public class SplitFunction implements Function
{
  private String regex;

  private Function input;

  private int index;

  private String[] params;

  public SplitFunction(Function input, Pattern p, int idx)
  {
    this.regex = p.toString();
    this.input = input;
    this.index = idx;
    this.params = input.parameters();
  }

  @Override
  public String getString(Values parameters)
  {
    String str = input.getString(parameters);
    if (str == Function.ERROR) return Function.ERROR;
    String[] a = str.split(regex);
    if (index < 0 || index >= a.length) return "";
    return a[index];
  }

  @Override
  public String[] parameters()
  {
    return params;
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