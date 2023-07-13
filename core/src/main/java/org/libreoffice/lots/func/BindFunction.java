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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.dialog.DialogLibrary;
import org.libreoffice.lots.util.L;

public class BindFunction implements Function
{
  private Map<String, Function> mapParamNameToSetFunction = new HashMap<>();

  private Function func;

  private String[] params;

  private Set<String> functionDialogReferences = new HashSet<>();

  public BindFunction(Function func, ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    this.func = func;

    Set<String> myParams = new HashSet<>(Arrays.asList(func.parameters()));
    Set<String> setFuncParams = new HashSet<>();

    ConfigThingy sets = conf.query("SET");
    Iterator<ConfigThingy> iter = sets.iterator();
    while (iter.hasNext())
    {
      ConfigThingy set = iter.next();
      if (set.count() != 2)
        throw new ConfigurationErrorException(
          L.m("BIND: SET requires exactly 2 parameters"));

      try
      {
        String name = set.getFirstChild().toString();
        Function setFunc = FunctionFactory.parse(set.getLastChild(), funcLib, dialogLib, context);

        if (mapParamNameToSetFunction.containsKey(name))
          throw new ConfigurationErrorException(
              L.m("BIND: The parameter {0} will be bound twice with SET", name));

        mapParamNameToSetFunction.put(name, setFunc);
        setFuncParams.addAll(Arrays.asList(setFunc.parameters()));
        setFunc.getFunctionDialogReferences(functionDialogReferences);

        /*
         * name has been bound so is no longer required as a parameter unless one of the
         * setFuncs requires the parameter. In this case the parameter is in setFuncParams
         * is captured and added back to myparams afterwards.
         */
        myParams.remove(name);
      }
      catch (NodeNotFoundException x)
      {
        // can't happen, tested count()
      }

    }

    /*
     * Add the parameters of the setFuncs to the required parameters and in String[]
     * convert.
     */
    myParams.addAll(setFuncParams);
    params = myParams.toArray(new String[0]);
  }

  @Override
  public String[] parameters()
  {
    return params;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    set.addAll(functionDialogReferences);
  }

  @Override
  public String getResult(Values parameters)
  {
    TranslatedValues trans = new TranslatedValues(parameters);
    String res = func.getResult(trans);
    if (trans.hasError) return FunctionLibrary.ERROR;
    return res;
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    TranslatedValues trans = new TranslatedValues(parameters);
    boolean res = func.getBoolean(trans);
    if (trans.hasError) return false;
    return res;
  }

  private class TranslatedValues implements Values
  {
    private Values values;

    private boolean hasError;

    public TranslatedValues(Values values)
    {
      this.values = values;
      hasError = false;
    }

    @Override
    public boolean hasValue(String id)
    {
      /*
       * DANGER! If the id is bound to a function, then we always return
       * true, even if the function may return an error. there would be 2
       * alternative behaviors: - only return true if
       * values.hasValue(id2) == true for all parameters of the function
       * expected. Disadvantage: Too strict on functions that take some arguments
       * are optional - run the function and see if it returns an error
       * provides disadvantage: the function is executed at a time when
       * this may not be expected. In addition, the function is once more
       * executed. This is not the case with external functions (especially basic macros).
       * desirable.
       */
      if (mapParamNameToSetFunction.containsKey(id)) return true;
      return (values.hasValue(id));
    }

    @Override
    public String getString(String id)
    {
      Function setFunc = mapParamNameToSetFunction.get(id);
      if (setFunc != null)
      {
        String res = setFunc.getResult(values);
        if (res == FunctionLibrary.ERROR)
        {
          hasError = true;
          return "";
        }
        return res;
      }
      return values.getString(id);
    }

    @Override
    public boolean getBoolean(String id)
    {
      Function setFunc = mapParamNameToSetFunction.get(id);
      if (setFunc != null)
      {
        String res = setFunc.getResult(values);
        if (res == FunctionLibrary.ERROR)
        {
          hasError = true;
          return false;
        }
        return res.equalsIgnoreCase("true");
      }
      return values.getBoolean(id);
    }
  }
}
