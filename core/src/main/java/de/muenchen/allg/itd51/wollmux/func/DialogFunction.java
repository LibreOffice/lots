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
package de.muenchen.allg.itd51.wollmux.func;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import de.muenchen.allg.itd51.wollmux.dialog.Dialog;

public class DialogFunction implements Function
{
  private Dialog dialog;

  private String dataName;

  private String dialogName;

  public DialogFunction(String dialogName, Dialog dialog, String dataName,
      Map<Object, Object> context)
  {
    this.dialog = dialog.instanceFor(context);
    this.dataName = dataName;
    this.dialogName = dialogName;
  }

  @Override
  public String[] parameters()
  {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    set.add(dialogName);
  }

  @Override
  public String getResult(Values parameters)
  {
    Object data = dialog.getData(dataName);
    if (data == null) return FunctionLibrary.ERROR;
    return data.toString();
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getResult(parameters).equalsIgnoreCase("true");
  }
}
