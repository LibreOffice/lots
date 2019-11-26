package de.muenchen.allg.itd51.wollmux.core.functions;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

abstract class NumberFunction extends MultiFunction
{
  protected char decimalPoint = '.';

  public NumberFunction(ConfigThingy conf, FunctionLibrary funcLib,
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
   * Startet eine neue Auswertung der Funktion für Parameter parameters. Im Falle
   * einer Summen-Funktion würde dies den Summenzähler mit 0 initialisieren.
   * 
   * @return Falls zu diesem Zeitpunkt bereits ein Ergebnis bestimmt werden kann
   *         (z.B. Function.ERROR, wenn ein benötigter Parameter nicht in
   *         parameters übergeben wurde), so wird dieses zurückgeliefert, ansonsten
   *         null.
   */
  protected abstract String initComputation(Values parameters);

  /**
   * Fügt den Wert num der aktuellen Berechnung hinzu. Im Falle einer
   * Summen-Funktion würde er auf den Summen-Zähler addiert. Darf eine Exception
   * werfen. In diesem Fall wird die Funktion Function.ERROR zurückliefern.
   * 
   * @return Falls zu diesem Zeitpunkt bereits ein Ergebnis bestimmt werden kann
   *         (z.B. im Falle einer Vergleichsfunktion, die Kurzschlussauswertung
   *         macht), so wird dieses zurückgeliefert, ansonsten null.
   */
  protected abstract String addToComputation(BigDecimal num);

  /**
   * Wird aufgerufen, nachdem der letzte Wert mittels addComputation() verarbeitet
   * wurde, wenn jeder addComputation()-Aufruf null geliefert hat.
   * 
   * @return das Endergebnis der Berechnung. null ist NICHT erlaubt.
   */
  protected abstract String computationResult();

  @Override
  public String getString(Values parameters)
  {
    String result = initComputation(parameters);
    if (result != null) return result;
    Iterator<Function> iter = subFunction.iterator();
    while (iter.hasNext())
    {
      Function func = iter.next();
      String str = func.getString(parameters);
      if (str == FunctionLibrary.ERROR) return FunctionLibrary.ERROR;
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
     * Falls der Dezimaltrenner nicht '.' ist, ersetzte alle '.' durch etwas, das
     * kein Dezimaltrenner ist, um eine NumberFormatException beim Konvertieren zu
     * provozieren. Dies ist eine Vorsichtsmaßnahme, da '.' zum Beispiel in
     * Deutschland alls Gruppierungszeichen verwendet wird und wir wollen nicht
     * fälschlicher weise "100.000" als 100 interpretieren, wenn die eingebende
     * Person 100000 gemeint hat.
     */
    if (decimalPoint != '.') str = str.replace('.', 'ß');

    return new BigDecimal(str.replace(decimalPoint, '.'));
  }

  /**
   * Liefert eine Stringrepräsentation von num
   */
  protected String formatBigDecimal(BigDecimal num)
  {
    /*
     * Workaround für Bug
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6480539
     * stripTrailingZeros() funktioniert nicht für 0.
     */
    String result;
    if (num.compareTo(BigDecimal.ZERO) == 0)
      result = "0";
    else
      result = num.stripTrailingZeros().toPlainString().replace('.', decimalPoint);
    return result;
  }
}