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
   * The subfunctions of conf are evaluated as BigDecimals and respectively
    * compared with the result of the first function via compareTo, where if
    * MARGIN is specified, whose distance &lt;= abs(MARGIN) is considered equal
    * becomes (equivalent to compareTo result 0). cmp1 and cmp2 are cancellation results
    * of this comparison. As soon as a compareTo returns cmp1 or cmp2, the breaks
    * Calculation and the result "false" is returned. If a compareTo
    * +1 returns and a later compareTo -1, so is aborted and as a result
    * "0" delivered. Similarly if a compareTo returns -1 and a later +1.
    * If result is not null, this string is returned as the result,
    * if not aborted with "false" or "0" as described above. If
    * result is zero, so if all compareTos returned 0, it becomes "true"
    * delivered. If at least one compareTo returned -1, it becomes "-1"
    * returned. If at least one compareTo returned +1, it becomes "1"
    * delivered. The case that one compareTo returned -1 and another +1
    * is intercepted earlier as described above.
    *
    * @throws ConfigurationErrorException
    *           unless at least 2 subfunctions are included in conf.
   */
  public NumberCompareFunction(int cmp1, int cmp2, String result,
      ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib,
      Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
    if (subFunction.size() < 2)
      throw new ConfigurationErrorException(L.m(
        "Function {0} requires at least 2 parameters", conf.getName()));
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
