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
import java.util.regex.Pattern;

public class SplitFunction implements Function
{
  private String regex;

  private Function input;

  private int index;

  private String[] params;

  public SplitFunction(Function input, Pattern p, int idx)
  {
    this.regex = p.toString();
    this.input = input;
    this.index = idx;
    this.params = input.parameters();
  }

  @Override
  public String getResult(Values parameters)
  {
    String str = input.getResult(parameters);
    if (str == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
    String[] a = str.split(regex);
    if (index < 0 || index >= a.length) return "";
    return a[index];
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
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getResult(parameters).equalsIgnoreCase("true");
  }
}
