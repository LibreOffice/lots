package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

public class BindFunction implements Function
{
  private Map<String, Function> mapParamNameToSetFunction =
    new HashMap<String, Function>();

  private Function func;

  private String[] params;

  private Set<String> functionDialogReferences = new HashSet<String>();

  public BindFunction(Function func, ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    this.func = func;

    Set<String> myParams = new HashSet<String>(Arrays.asList(func.parameters()));
    Set<String> setFuncParams = new HashSet<String>();

    ConfigThingy sets = conf.query("SET");
    Iterator<ConfigThingy> iter = sets.iterator();
    while (iter.hasNext())
    {
      ConfigThingy set = iter.next();
      if (set.count() != 2)
        throw new ConfigurationErrorException(
          L.m("BIND: SET benötigt genau 2 Parameter"));

      String name;
      Function setFunc;
      try
      {
        name = set.getFirstChild().toString();
        setFunc = FunctionFactory.parse(set.getLastChild(), funcLib, dialogLib, context);
      }
      catch (NodeNotFoundException x)
      { // kann nicht passieren, hab count() getestet
        name = null;
        setFunc = null;
      }

      if (mapParamNameToSetFunction.containsKey(name))
        throw new ConfigurationErrorException(L.m(
          "BIND: Der Parameter %1 wird 2 mal mit SET gebunden", name));

      mapParamNameToSetFunction.put(name, setFunc);
      setFuncParams.addAll(Arrays.asList(setFunc.parameters()));
      setFunc.getFunctionDialogReferences(functionDialogReferences);

      /*
       * name wurde gebunden, wird also nicht mehr als Parameter benötigt, außer
       * wenn eine der setFuncs den Parameter benötigt. In diesem Fall ist der
       * Parameter in setFuncParams erfasst und wird nachher wieder zu myparams
       * hinzugefügt.
       */
      myParams.remove(name);
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
  public String getString(Values parameters)
  {
    TranslatedValues trans = new TranslatedValues(parameters);
    String res = func.getString(trans);
    if (trans.hasError) return Function.ERROR;
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

    public boolean hasError;

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
        String res = setFunc.getString(values);
        if (res == Function.ERROR)
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
        String res = setFunc.getString(values);
        if (res == Function.ERROR)
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