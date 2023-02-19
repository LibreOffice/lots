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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;

abstract class NumberFunction extends MultiFunction
{
  protected char decimalPoint = '.';

  protected NumberFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
    try
    {
      decimalPoint = ((DecimalFormat) NumberFormat.getInstance()).getDecimalFormatSymbols()
          .getDecimalSeparator();
    } catch (Exception x)
    {
      decimalPoint = '.';
    }
  }

  /**
   * Starts a new evaluation of the function for parameters parameters. In the event of
   * a sum function, this would initialize the sum counter to 0.
   *
   * @return If a result can already be determined at this point
   *         (e.g. Function.ERROR if a required parameter is not in
   *         parameters was passed), this is returned, otherwise
   *         null.
   */
  protected abstract String initComputation(Values parameters);

  /**
   * Adds the value num to the current calculation. In case of a
   * Sum function it would be added to the sum counter. May throw an exception
   * throw. In this case the function will return Function.ERROR.
   *
   * @return If a result can already be determined at this point
   *         (e.g. in the case of a comparison function, the short-circuit evaluation
   *         does), this is returned, otherwise null.
   */
  protected abstract String addToComputation(BigDecimal num);

  /**
   * Called after the last value is processed using addComputation()
   * became if every addComputation() call returned null.
   *
   * @return the end result of the calculation. null is NOT allowed.
   */
  protected abstract String computationResult();

  @Override
  public String getResult(Values parameters)
  {
    String result = initComputation(parameters);
    if (result != null) return result;
    Iterator<Function> iter = subFunction.iterator();
    while (iter.hasNext())
    {
      Function func = iter.next();
      String str = func.getResult(parameters);
      if (str.equals(FunctionLibrary.ERROR)) return FunctionLibrary.ERROR;
      try
      {
        BigDecimal num = makeBigDecimal(str);
        result = addToComputation(num);
        if (result != null) return result;
      }
      catch (Exception x)
      {
        return FunctionLibrary.ERROR;
      }
    }
    return computationResult();
  }

  protected BigDecimal makeBigDecimal(String str)
  {
    /*
     * If the decimal separator is not '.' is replaced all '.' through something that
     * is not a decimal separator in order to throw a NumberFormatException when converting
     * provoke. This is a precaution because '.' for example in
     * Germany alls grouping character is used and we don't want it
     * incorrectly interpreting "100,000" as 100 when entering
     * person meant 100000.
     */
    if (decimalPoint != '.') str = str.replace('.', 'ß');

    return new BigDecimal(str.replace(decimalPoint, '.'));
  }

  /**
   * Returns a string representation of num
   */
  protected String formatBigDecimal(BigDecimal num)
  {
    /*
     * Workaround for bug
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6480539
     * stripTrailingZeros() does not work for 0.
     */
    String result;
    if (num.compareTo(BigDecimal.ZERO) == 0)
      result = "0";
    else
      result = num.stripTrailingZeros().toPlainString().replace('.', decimalPoint);
    return result;
  }
}
