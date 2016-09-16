package de.muenchen.allg.itd51.wollmux.func.functions;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

public class NumberCompareFunction extends NumberFunction
{
  private BigDecimal compare;

  private BigDecimal lowBound;

  private BigDecimal highBound;

  private int cmp1;

  private int cmp2;

  private int prevCompare;

  private String result;

  private BigDecimal margin;

  Function marginFun;

  /**
   * Die Unterfunktionen von conf werden als BigDecimals ausgewertet und jeweils
   * mit dem Ergebnis der ersten Funktion verglichen via compareTo, wobei falls
   * MARGIN angegeben ist, dessen Abstand <= abs(MARGIN) ist als gleich angesehen
   * wird (entspricht compareTo Ergebnis 0). cmp1 und cmp2 sind Abbruchergebnisse
   * dieses Vergleichs. Sobald ein compareTo cmp1 oder cmp2 liefert bricht die
   * Berechnung ab und es wird das Ergebnis "false" geliefert. Falls ein compareTo
   * +1 liefert und ein späteres compareTo -1, so wird abgebrochen und als Ergebnis
   * "0" geliefert. Analog wenn ein compareTo -1 liefert und eine späteres +1.
   * Falls result nicht null ist, so wird dieser String als Ergebnis geliefert,
   * wenn nicht wie oben beschrieben mit "false" oder "0" abgebrochen wurde. Falls
   * result null ist, so wird falls alle compareTos 0 geliefert haben "true"
   * geliefert. Falls mindestens ein compareTo -1 geliefert hat wird "-1"
   * zurückgeliefert. Falls mindestens ein compareTo +1 geliefert hat wird "1"
   * geliefert. Der Fall, dass ein compareTo -1 und ein anderer +1 geliefert hat
   * wird wie bereits oben beschrieben schon früher abgefangen.
   * 
   * @throws ConfigurationErrorException
   *           falls nicht mindestens 2 Unterfunktionen in conf enthalten sind.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public NumberCompareFunction(int cmp1, int cmp2, String result,
      ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib,
      Map<Object, Object> context) throws ConfigurationErrorException
  {
    super(conf, funcLib, dialogLib, context);
    if (subFunction.size() < 2)
      throw new ConfigurationErrorException(L.m(
        "Funktion %1 erfordert mindestens 2 Parameter", conf.getName()));
    this.cmp1 = cmp1;
    this.cmp2 = cmp2;
    this.result = result;
  }

  @Override
  protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    if (conf.getName().equals("MARGIN"))
    {
      if (conf.count() != 1)
        throw new ConfigurationErrorException(
          L.m("MARGIN muss genau eine Funktion enthalten"));
      marginFun = FunctionFactory.parseChildren(conf, funcLib, dialogLib, context);
      return true;
    }
    else
      return false;
  }

  @Override
  protected String[] getAdditionalParams()
  {
    if (marginFun != null)
      return marginFun.parameters();
    else
      return null;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    super.getFunctionDialogReferences(set);
    if (marginFun != null) marginFun.getFunctionDialogReferences(set);
  }

  @Override
  protected String initComputation(Values parameters)
  {
    compare = null;
    prevCompare = 0;
    if (marginFun != null)
    {
      String str = marginFun.getString(parameters);
      if (str == Function.ERROR) return Function.ERROR;
      try
      {
        margin = makeBigDecimal(str);
        margin = margin.abs();
      }
      catch (Exception x)
      {
        return Function.ERROR;
      }
    }
    else
      margin = BigDecimal.ZERO;

    return null;
  }

  @Override
  protected String addToComputation(BigDecimal num)
  {
    if (compare == null)
    {
      compare = num;
      lowBound = compare.subtract(margin);
      highBound = compare.add(margin);
      return null;
    }

    int res;
    if (lowBound.compareTo(num) <= 0 && num.compareTo(highBound) <= 0)
      res = 0;
    else
      res = compare.compareTo(num);

    if (res == cmp1 || res == cmp2) return "false";
    if (res * prevCompare < 0) return "0";
    prevCompare += res;

    return null;
  }

  @Override
  protected String computationResult()
  {
    if (result != null) return result;
    switch (Integer.signum(prevCompare))
    {
      case 1:
        return "1";
      case -1:
        return "-1";
      default:
        return "true";
    }
  }
}