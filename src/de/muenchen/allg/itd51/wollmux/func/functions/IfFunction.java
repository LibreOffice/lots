package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

public class IfFunction implements Function
{
  private Function ifFunction;

  private Function thenFunction;

  private Function elseFunction;

  private String[] params;

  public IfFunction(Function ifFunction, Function thenFunction,
      Function elseFunction)
  {
    Set<String> myparams = new HashSet<String>();
    myparams.addAll(Arrays.asList(ifFunction.parameters()));
    myparams.addAll(Arrays.asList(thenFunction.parameters()));
    myparams.addAll(Arrays.asList(elseFunction.parameters()));
    params = myparams.toArray(new String[0]);
    this.ifFunction = ifFunction;
    this.thenFunction = thenFunction;
    this.elseFunction = elseFunction;
  }

  @Override
  public String[] parameters()
  {
    return params;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    ifFunction.getFunctionDialogReferences(set);
    thenFunction.getFunctionDialogReferences(set);
    elseFunction.getFunctionDialogReferences(set);
  }

  @Override
  public String getString(Values parameters)
  {
    String condition = ifFunction.getString(parameters);
    if (condition == Function.ERROR) return Function.ERROR;
    if (condition.equalsIgnoreCase("true"))
      return thenFunction.getString(parameters);
    else
      return elseFunction.getString(parameters);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    String condition = ifFunction.getString(parameters);
    if (condition == Function.ERROR) return false;
    if (condition.equalsIgnoreCase("true"))
      return thenFunction.getBoolean(parameters);
    else
      return elseFunction.getBoolean(parameters);
  }
}