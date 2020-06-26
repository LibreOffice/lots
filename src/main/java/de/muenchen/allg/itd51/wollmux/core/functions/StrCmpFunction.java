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

import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.util.L;

public class StrCmpFunction extends MultiFunction
{
  public StrCmpFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
    if (subFunction.size() < 2)
      throw new ConfigurationErrorException(L.m(
        "Funktion %1 erfordert mindestens 2 Parameter", conf.getName()));
  }

  @Override
  public String getString(Values parameters)
  {
    Iterator<Function> iter = subFunction.iterator();
    Function func = iter.next();
    String compare = func.getString(parameters);
    if (compare == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
    int prevCompare = 0;
    while (iter.hasNext())
    {
      func = iter.next();
      String str = func.getString(parameters);
      if (str == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
      int res = Integer.signum(compare.compareTo(str));
      if (res * prevCompare < 0) return "0";
      prevCompare += res;
    }

    switch (Integer.signum(prevCompare))
    {
      case -1:
        return "-1";
      case 1:
        return "1";
      default:
        return "true";
    }
  }
}