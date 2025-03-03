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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ReplaceFunction implements Function
{
  private Pattern pattern;

  private Function input;

  private Function replace;

  private String[] params;

  public ReplaceFunction(Function input, Pattern p, Function replace)
  {
    pattern = p;
    this.input = input;
    this.replace = replace;
    Set<String> paramset = new HashSet<>();
    paramset.addAll(Arrays.asList(input.parameters()));
    paramset.addAll(Arrays.asList(replace.parameters()));
    this.params = paramset.toArray(new String[] {});
  }

  @Override
  public String getResult(Values parameters)
  {
    String str = input.getResult(parameters);
    String repStr = replace.getResult(parameters);
    if (str == FunctionLibrary.ERROR || repStr == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
    return pattern.matcher(str).replaceAll(repStr);
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
    replace.getFunctionDialogReferences(set);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getResult(parameters).equalsIgnoreCase("true");
  }
}
