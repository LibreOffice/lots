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
package de.muenchen.allg.itd51.wollmux.func;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DivideFunction implements Function
{
  private Function dividendFunction;

  private Function divisorFunction = null;

  private int minScale;

  private int maxScale;

  private String[] params;

  /**
   * If divisorFunction is zero, 1 is assumed.
   */
  public DivideFunction(Function dividendFunction, Function divisorFunction,
      int minScale, int maxScale)
  {
    Set<String> myparams = new HashSet<>();
    myparams.addAll(Arrays.asList(dividendFunction.parameters()));
    if (divisorFunction != null)
      myparams.addAll(Arrays.asList(divisorFunction.parameters()));
    params = myparams.toArray(new String[0]);
    this.dividendFunction = dividendFunction;
    this.divisorFunction = divisorFunction;
    this.minScale = minScale;
    this.maxScale = maxScale;
  }

  @Override
  public String[] parameters()
  {
    return params;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    dividendFunction.getFunctionDialogReferences(set);
    if (divisorFunction != null) divisorFunction.getFunctionDialogReferences(set);
  }

  @Override
  public String getResult(Values parameters)
  { // TESTED
    char decimalPoint;
    try
    {
      decimalPoint =
        ((DecimalFormat) NumberFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();
    }
    catch (Exception x)
    {
      decimalPoint = '.';
    }

    String dividend = dividendFunction.getResult(parameters);
    if (dividend == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;

    String divisor = "1";
    if (divisorFunction != null) divisor = divisorFunction.getResult(parameters);
    if (divisor == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;

    /*
     * If the decimal separator is not '.' is replaced all '.' through something that
     * is not a decimal separator in order to throw a NumberFormatException when converting
     * provoke. This is a precaution because '.' for example in
     * Germany alls grouping character is used and we don't want it
     * incorrectly interpreting "100,000" as 100 when entering
     * person meant 100000.
     */
    if (decimalPoint != '.')
    {
      dividend = dividend.replace('.', 'ß');
      divisor = divisor.replace('.', 'ß');
    }

    BigDecimal bigResult;
    try
    {
      BigDecimal bigDividend = new BigDecimal(dividend.replace(decimalPoint, '.'));
      BigDecimal bigDivisor = new BigDecimal(divisor.replace(decimalPoint, '.'));

      bigResult = bigDividend.divide(bigDivisor, maxScale, RoundingMode.HALF_UP);
    }
    catch (Exception x)
    {
      return FunctionLibrary.ERROR;
    }

    /*
     * Unfortunately, NumberFormat cannot be used for formatting, since it is only
     * has the precision of double (according to Java Doc).
     */

    /*
     * Workaround for bug
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6480539
     * stripTrailingZeros() does not work for 0.
     */
    String result;
    if (bigResult.compareTo(BigDecimal.ZERO) == 0)
      result = "0";
    else
      result = bigResult.stripTrailingZeros().toPlainString();

    StringBuilder buffy = new StringBuilder(result);
    int idx = result.indexOf('.');
    if (idx == 0)
    {
      buffy.insert(0, "0");
      idx = 1;
    }
    if (idx < 0 && minScale > 0)
    {
      buffy.append(".0");
      idx = buffy.length() - 2;
    }

    int decimalDigits = (idx < 0) ? 0 : buffy.length() - idx - 1;
    for (int i = decimalDigits; i < minScale; ++i)
      buffy.append('0');

    result = buffy.toString().replace('.', decimalPoint);
    return result;
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return false;
  }
}
