/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.util.L;

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
   * MARGIN angegeben ist, dessen Abstand &lt;= abs(MARGIN) ist als gleich angesehen
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
   */
  public NumberCompareFunction(int cmp1, int cmp2, String result,
      ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib,
      Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
    if (subFunction.size() < 2)
      throw new ConfigurationErrorException(L.m(
        "Function %1 requires at least 2 parameters", conf.getName()));
    this.cmp1 = cmp1;
    this.cmp2 = cmp2;
    this.result = result;
  }

  @Override
  protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.getName().equals("MARGIN"))
    {
      if (conf.count() != 1)
        throw new ConfigurationErrorException(
          L.m("MARGIN must contain exactly one function"));
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
      return ArrayUtils.EMPTY_STRING_ARRAY;
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
      String str = marginFun.getResult(parameters);
      if (str == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
      try
      {
        margin = makeBigDecimal(str);
        margin = margin.abs();
      }
      catch (Exception x)
      {
        return FunctionLibrary.ERROR;
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
