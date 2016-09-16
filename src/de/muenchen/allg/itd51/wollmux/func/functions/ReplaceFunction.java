package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

public class ReplaceFunction implements Function
{
  private Pattern pattern;

  private Function input;

  private Function replace;

  private String[] params;

  public ReplaceFunction(Function input, Pattern p, Function replace)
  {
    pattern = p;
    this.input = input;
    this.replace = replace;
    Set<String> paramset = new HashSet<String>();
    paramset.addAll(Arrays.asList(input.parameters()));
    paramset.addAll(Arrays.asList(replace.parameters()));
    this.params = paramset.toArray(new String[] {});
  }

  @Override
  public String getString(Values parameters)
  {
    String str = input.getString(parameters);
    String repStr = replace.getString(parameters);
    if (str == Function.ERROR || repStr == Function.ERROR) return Function.ERROR;
    return pattern.matcher(str).replaceAll(repStr);
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
    replace.getFunctionDialogReferences(set);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}