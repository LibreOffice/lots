/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;

public class SelectFunction extends MultiFunction
{
  private Function onErrorFunction;

  public SelectFunction(Collection<Function> subFunction)
  {
    super(subFunction);
  }

  public SelectFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.getName().equals("ONERROR"))
    {
      onErrorFunction = new CatFunction(conf, funcLib, dialogLib, context);
      return true;
    }
    return false;
  }

  @Override
  protected String[] getAdditionalParams()
  {
    if (onErrorFunction != null)
      return onErrorFunction.parameters();
    else
      return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    super.getFunctionDialogReferences(set);
    if (onErrorFunction != null) onErrorFunction.getFunctionDialogReferences(set);
  }

  @Override
  public String getString(Values parameters)
  {
    Iterator<Function> iter = subFunction.iterator();
    String result = FunctionLibrary.ERROR;
    while (iter.hasNext())
    {
      Function func = iter.next();
      String str = func.getString(parameters);
      if (str != FunctionLibrary.ERROR)
      {
        result = str;
        if (str.length() > 0) break;
      }
      else if (onErrorFunction != null)
      {
        return onErrorFunction.getString(parameters);
      }
    }
    return result;
  }
}