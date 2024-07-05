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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.dialog.DialogLibrary;
import org.libreoffice.lots.util.L;

public abstract class MultiFunction implements Function
{
  protected Collection<Function> subFunction;

  private String[] params;

  public MultiFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    List<Function> subFunc = new ArrayList<>(conf.count());
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy subFunConf = iter.next();
      if (handleParam(subFunConf, funcLib, dialogLib, context)) continue;
      Function fun = FunctionFactory.parse(subFunConf, funcLib, dialogLib, context);
      subFunc.add(fun);
    }

    if (subFunc.isEmpty())
      throw new ConfigurationErrorException(L.m(
        "Function {0} requires at least one parameter", conf.getName()));

    init(subFunc);
  }

  /**
   * Returns true if conf has already been fully handled by handleParam
   * and should no longer be recorded as a subfunction. This function is provided by
   * Overridden subclasses to handle special parameters. DANGER! Becomes
   * overridden this method, so are normally too
   * {@link #getAdditionalParams()} and
   * Override {@link #getFunctionDialogReferences(Collection)} to get the
   * Handle additional functions from the handled parameters.
   */
  @SuppressWarnings("squid:S1172")
  protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    return false;
  }

  /**
   * Returns the names of the parameters of the additional functions provided by
   * {@link #handleParam(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
   * have been parsed, or null if there are none.
   */
  protected String[] getAdditionalParams()
  {
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  public MultiFunction(Collection<Function> subFunction)
  {
    init(subFunction);
  }

  private void init(Collection<Function> subFunction)
  {
    this.subFunction = subFunction;

    // A set would be more performant, but this way the order is preserved. Possibly.
    // Does it make sense to include the other Function classes that are currently Sets
    // use switch to construct with list and contains() test and
    // thereby achieving a well-defined order. Currently the need
    // but not given for this, especially since insertFunctionValue was rewritten
    // should be and the FM4000 has already been rewritten so as not to be affected by the
    // Depend order.
    ArrayList<String> deps = new ArrayList<>();
    for (Function f : subFunction)
    {
      String[] parameter = f.parameters();
      for (String str : parameter)
      {
        if (!deps.contains(str)) deps.add(str);
      }
    }

    for (String str : getAdditionalParams())
    {
      if (!deps.contains(str))
      {
        deps.add(str);
      }
    }

    params = new String[deps.size()];
    params = deps.toArray(params);
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getResult(parameters).equalsIgnoreCase("true");
  }

  @Override
  public String[] parameters()
  {
    return params;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    Iterator<Function> iter = subFunction.iterator();
    while (iter.hasNext())
    {
      iter.next().getFunctionDialogReferences(set);
    }
  }
}
