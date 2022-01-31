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

public class ValueFunction implements Function
{
  String[] params;

  public ValueFunction(String valueName)
  {
    params = new String[] { valueName };
  }

  @Override
  public String[] parameters()
  {
    return params;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    // Value Function hat keine Referenzen auf Dialoge.
  }

  @Override
  public String getString(Values parameters)
  {
    if (!parameters.hasValue(params[0])) return FunctionLibrary.ERROR;
    return parameters.getString(params[0]);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}
