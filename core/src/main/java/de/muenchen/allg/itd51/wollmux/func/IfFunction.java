/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class IfFunction implements Function
{
  private Function ifFunction;

  private Function thenFunction;

  private Function elseFunction;

  private String[] params;

  public IfFunction(Function ifFunction, Function thenFunction,
      Function elseFunction)
  {
    Set<String> myparams = new HashSet<>();
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
    if (condition == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
    if (condition.equalsIgnoreCase("true"))
      return thenFunction.getString(parameters);
    else
      return elseFunction.getString(parameters);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    String condition = ifFunction.getString(parameters);
    if (condition == FunctionLibrary.ERROR) return false;
    if (condition.equalsIgnoreCase("true"))
      return thenFunction.getBoolean(parameters);
    else
      return elseFunction.getBoolean(parameters);
  }
}
