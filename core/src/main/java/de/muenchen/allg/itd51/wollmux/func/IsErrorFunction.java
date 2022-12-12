/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import java.util.Collection;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.util.L;

public class IsErrorFunction implements Function
{
  private Function func;

  private boolean objectCompare;

  /**
   * Falls objectCompare == true, wird == Function,ERROR getestet, ansonsten
   * equals(Function,ERROR).
   */
  public IsErrorFunction(boolean objectCompare, ConfigThingy conf,
      FunctionLibrary funcLib, DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.count() != 1)
      throw new ConfigurationErrorException(L.m(
        "Function {0} must have exactly one parameter", conf.getName()));

    this.objectCompare = objectCompare;
    func = FunctionFactory.parseChildren(conf, funcLib, dialogLib, context);
  }

  @Override
  public String[] parameters()
  {
    return func.parameters();
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    func.getFunctionDialogReferences(set);
  }

  @Override
  public String getResult(Values parameters)
  {
    String str = func.getResult(parameters);
    if (isError(str))
      return "true";
    else
      return "false";
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    String str = func.getResult(parameters);
    return isError(str);
  }

  private boolean isError(String str)
  {
    if (objectCompare)
    {
      return FunctionLibrary.ERROR == str;
    }
    else
    {
      return FunctionLibrary.ERROR.equals(str);
    }
  }

}
