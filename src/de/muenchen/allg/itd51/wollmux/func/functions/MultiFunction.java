package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

abstract class MultiFunction implements Function
{
  protected Collection<Function> subFunction;

  private String[] params;

  public MultiFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    Vector<Function> subFunction = new Vector<Function>(conf.count());
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy subFunConf = iter.next();
      if (handleParam(subFunConf, funcLib, dialogLib, context)) continue;
      Function fun = FunctionFactory.parse(subFunConf, funcLib, dialogLib, context);
      subFunction.add(fun);
    }

    if (subFunction.size() == 0)
      throw new ConfigurationErrorException(L.m(
        "Funktion %1 erfordert mindestens einen Parameter", conf.getName()));

    subFunction.trimToSize();
    init(subFunction);
  }

  /**
   * Liefert true, wenn conf von handleParam bereits vollständig behandelt wurde
   * und nicht mehr als Subfunktion erfasst werden soll. Diese Funktion wird von
   * Unterklassen überschrieben, um spezielle Parameter zu behandeln. ACHTUNG! Wird
   * diese Methode überschrieben, so sind normalerweise auch
   * {@link #getAdditionalParams()} und
   * {@link #getFunctionDialogReferences(Collection)} zu überschreiben, um die
   * zusätzlichen Funktionen aus dem behandelten Parameter zu behandeln.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    return false;
  }

  /**
   * Liefert die Namen der Parameter der zusätzlichen Funktionen, die von
   * {@link #handleParam(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
   * geparst wurden oder null, falls es keine gibt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected String[] getAdditionalParams()
  {
    return null;
  }

  public MultiFunction(Collection<Function> subFunction)
  {
    init(subFunction);
  }

  private void init(Collection<Function> subFunction)
  {
    this.subFunction = subFunction;

    // Ein Set wäre performanter, aber so wird die Reihenfolge beibehalten. Evtl.
    // macht es Sinn, auch die anderen Function-Klassen, die im Moment Sets
    // verwenden auf das Konstrukt mit Liste und contains()-Test umzustellen und
    // dadurch eine wohldefinierte Reihenfolge zu erreichen. Derzeit ist der Bedarf
    // dafür aber nicht gegeben, insbes. da insertFunctionValue umgeschrieben
    // werden soll und der FM4000 bereits umgeschrieben wurde um nicht von der
    // Reihenfolge abzuhängen.
    ArrayList<String> deps = new ArrayList<String>();
    for (Function f : subFunction)
    {
      String[] params = f.parameters();
      for (String str : params)
      {
        if (!deps.contains(str)) deps.add(str);
      }
    }

    String[] additionalparams = getAdditionalParams();
    if (additionalparams != null)
    {
      for (String str : additionalparams)
      {
        if (!deps.contains(str)) deps.add(str);
      }
    }

    params = new String[deps.size()];
    params = deps.toArray(params);
  }

  @Override
  public abstract String getString(Values parameters);

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
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