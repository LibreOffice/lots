package de.muenchen.allg.itd51.wollmux.func.functions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;

public class DivideFunction implements Function
{
  private Function dividendFunction;

  private Function divisorFunction = null;

  private int minScale;

  private int maxScale;

  private String[] params;

  /**
   * Wenn divisorFunction null ist wird 1 angenommen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public DivideFunction(Function dividendFunction, Function divisorFunction,
      int minScale, int maxScale)
  {
    Set<String> myparams = new HashSet<String>();
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
  public String getString(Values parameters)
  { // TESTED
    char decimalPoint = '.';
    try
    {
      decimalPoint =
        ((DecimalFormat) NumberFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();
    }
    catch (Exception x)
    {}
    ;

    String dividend = dividendFunction.getString(parameters);
    if (dividend == Function.ERROR) return Function.ERROR;

    String divisor = "1";
    if (divisorFunction != null) divisor = divisorFunction.getString(parameters);
    if (divisor == Function.ERROR) return Function.ERROR;

    /*
     * Falls der Dezimaltrenner nicht '.' ist, ersetzte alle '.' durch etwas, das
     * kein Dezimaltrenner ist, um eine NumberFormatException beim Konvertieren zu
     * provozieren. Dies ist eine Vorsichtsmaßnahme, da '.' zum Beispiel in
     * Deutschland alls Gruppierungszeichen verwendet wird und wir wollen nicht
     * fälschlicher weise "100.000" als 100 interpretieren, wenn die eingebende
     * Person 100000 gemeint hat.
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
      return Function.ERROR;
    }

    /*
     * NumberFormat kann leider nicht zum formatieren verwendet werden, da es nur
     * die Genauigkeit von double hat (laut Java Doc).
     */

    /*
     * Workaround für Bug
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6480539
     * stripTrailingZeros() funktioniert nicht für 0.
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