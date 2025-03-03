/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.func;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.dialog.DialogLibrary;

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
  public String getResult(Values parameters)
  {
    Iterator<Function> iter = subFunction.iterator();
    String result = FunctionLibrary.ERROR;
    while (iter.hasNext())
    {
      Function func = iter.next();
      String str = func.getResult(parameters);
      if (str != FunctionLibrary.ERROR)
      {
        result = str;
        if (str.length() > 0) break;
      }
      else if (onErrorFunction != null)
      {
        return onErrorFunction.getResult(parameters);
      }
    }
    return result;
  }
}
