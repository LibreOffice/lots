/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.dialog.DialogLibrary;

public class NotFunction extends MultiFunction
{

  public NotFunction(Collection<Function> subFunction)
  {
    super(subFunction);
  }

  public NotFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  public String getResult(Values parameters)
  {
    Iterator<Function> iter = subFunction.iterator();
    while (iter.hasNext())
    {
      Function func = iter.next();
      String str = func.getResult(parameters);
      if (str == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
      if (!str.equalsIgnoreCase("true")) return "true";
    }
    return "false";
  }
}
