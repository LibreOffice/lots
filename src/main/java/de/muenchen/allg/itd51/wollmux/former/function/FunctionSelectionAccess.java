package de.muenchen.allg.itd51.wollmux.former.function;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Interface zum Zugriff auf eine FunctionSelection.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FunctionSelectionAccess
{

  /**
   * Dieser spezielle Funktionsname signalisiert, dass keine Funktion ausgewählt ist.
   */
  public static final String NO_FUNCTION = L.m("<keine>");

  /**
   * Dieser spezielle Funktionsname signalisiert, dass der Benutzer die Funktion
   * manuell eingegeben hat und sie direkt in dieser FunctionSelection gespeichert
   * ist.
   */
  public static final String EXPERT_FUNCTION = L.m("<Experte>");

  /**
   * Liefert true gdw diese FunctionSelection eine Referenz auf eine benannte
   * Funktion ist, d,h, wenn {@link #getFunctionName()} einen sinnvollen Namen
   * liefert.
   */
  public boolean isReference();

  /**
   * Liefert true gdw diese FunctionSelection eine vom Benutzer manuell eingegebene
   * Funktion ist, die von {@link #getExpertFunction()} zurückgeliefert werden kann.
   */
  public boolean isExpert();

  /**
   * Liefert true gdw, keine Funktion ausgewählt ist.
   */
  public boolean isNone();

  /**
   * Liefert true gdw diese FunctionSelection eine Referenz auf eine benannte
   * Funktion ist und für mindestens einen Parameter dieser Funktion einen Wert
   * spezifiziert.
   */
  public boolean hasSpecifiedParameters();

  /**
   * Liefert den Namen der Funktion, falls es eine Referenz auf eine externe Funktion
   * ist, oder {@link #NO_FUNCTION} bzw, {@link #EXPERT_FUNCTION}.
   */
  public String getFunctionName();

  /**
   * Liefert die Namen der Funktionsparameter, die die momentan ausgewählte Funktion
   * erwartet.
   */
  public String[] getParameterNames();

  /**
   * Nimmt eine Abbildung von Parameternamen (Strings) auf Parameterwerte ({@link ParamValue}s)
   * und übernimmt diese direkt als Referenz.
   * 
   * @param mapNameToParamValue
   *          wird direkt als Referenz übernommen.
   */
  public void setParameterValues(Map<String, ParamValue> mapNameToParamValue);

  /**
   * Liefert den für Parameter paramName gesetzten Wert. Ist für paramName kein Wert
   * gesetzt, so wird dennoch ein ParamValue geliefert, jedoch eines für das
   * {@link ParamValue#isUnspecified()} true liefert. Das zurückgelieferte Objekt ist
   * kann vom Aufrufer verändert werden, ohne Auswirkungen auf das
   * FunctionSelectionAccess Objekt.
   */
  public ParamValue getParameterValue(String paramName);

  /**
   * Setzt den Wert für Parameter paramName auf paramValue.
   * 
   * @param paramValue
   *          wird direkt als Referenz in die internen Datenstrukturen übernommen,
   *          darf also vom Aufrufer nachher nicht mehr geändert werden.
   */
  public void setParameterValue(String paramName, ParamValue paramValue);

  /**
   * Ändert den Namen der Funktion auf functionName und die Liste der Namen ihrer
   * Parameter auf paramNames.
   * 
   * @param functionName
   *          hier können auch die Spezialnamen {@link #NO_FUNCTION} oder
   *          {@link #EXPERT_FUNCTION} verwendet werden.
   * @param paramNames
   *          wird ignoriert, falls {@link #NO_FUNCTION} oder
   *          {@link #EXPERT_FUNCTION} übergeben wird. Wird ansonsten kopiert, nicht
   *          direkt als Referenz verwendet.
   */
  public void setFunction(String functionName, String[] paramNames);

  /**
   * Liefert eine Kopie der gespeicherten vom Benutzer manuell eingegebenen Funktion.
   * Ist keine gesetzt, so wird ein ConfigThingy ohne Kinder zurückgeliefert.
   * 
   * @return ein Objekt, das der Aufrufer ändern darf.
   */
  public ConfigThingy getExpertFunction();

  /**
   * Schaltet den Typ dieser FunctionSelection auf {@link #EXPERT_FUNCTION} und setzt
   * die zugehörige Funktionsdefinition auf conf. conf muss einen beliebigen
   * Wurzelknoten haben, wie z.B. "PLAUSI", "AUTOFILL" o.ä. der noch keine
   * Grundfunktion ist.
   * 
   * @param funConf
   *          wird kopiert, nicht als Referenz übernommen.
   */
  public void setExpertFunction(ConfigThingy funConf);

}
