/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.util.L;

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
         * name wurde gebunden, wird also nicht mehr als Parameter benötigt, außer wenn eine der
         * setFuncs den Parameter benötigt. In diesem Fall ist der Parameter in setFuncParams
         * erfasst und wird nachher wieder zu myparams hinzugefügt.
         */
        myParams.remove(name);
      }
      catch (NodeNotFoundException x)
      {
        // kann nicht passieren, hab count() getestet
      }

    }

    /*
     * Parameter der setFuncs den benötigten Parametern hinzufügen und in String[]
     * konvertieren.
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
       * ACHTUNG! Wenn die id an eine Funktion gebunden ist, dann liefern wir immer
       * true, auch wenn die Funktion evtl. einen Fehler liefert. Es gäbe 2
       * alternative Verhaltensweisen: - nur true liefern, wenn
       * values.hasValue(id2) == true für alle Parameter, die die Funktion
       * erwartet. Nachteil: Zu strikt bei Funktionen, bei denen manche Argumente
       * optional sind - die Funktion ausführen und sehen, ob sie einen Fehler
       * liefert Nachteil: Die Funktion wird zu einem Zeitpunkt ausgeführt, zu dem
       * dies evtl. nicht erwartet wird. Außerdem wird die Funktion einmal mehr
       * ausgeführt. Bei externen Funktionen (insbes. Basic-Makros) ist dies nicht
       * wünschenswert.
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
