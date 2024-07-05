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
import java.util.regex.Pattern;

public class MatchFunction implements Function
{
  private Pattern pattern;

  private Function input;

  public MatchFunction(Function input, Pattern p)
  {
    pattern = p;
    this.input = input;
  }

  @Override
  public String getResult(Values parameters)
  {
    String str = input.getResult(parameters);
    if (str == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
    if (pattern.matcher(str).matches()) return "true";
    return "false";
  }

  @Override
  public String[] parameters()
  {
    return input.parameters();
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
