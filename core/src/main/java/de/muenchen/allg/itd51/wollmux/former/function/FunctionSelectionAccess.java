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
package de.muenchen.allg.itd51.wollmux.former.function;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;

/**
 * Interface zum Zugriff auf eine FunctionSelection.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FunctionSelectionAccess
{

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
   * Liefert den Namen der Funktion.
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
   * Ändert den Namen der Funktion auf functionName und die Liste der Namen ihrer Parameter auf
   * paramNames.
   *
   * @param functionName
   *          Neuer Name der Funktion.
   * @param paramNames
   *          Neue Parameternamen werden kopiert, nicht direkt als Referenz verwendet.
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
   * Setzt die zugehörige Funktionsdefinition auf conf. conf muss einen beliebigen Wurzelknoten
   * haben, wie z.B. "PLAUSI", "AUTOFILL" o.ä. der noch keine Grundfunktion ist.
   *
   * @param funConf
   *          wird kopiert, nicht als Referenz übernommen.
   */
  public void setExpertFunction(ConfigThingy funConf);

}
