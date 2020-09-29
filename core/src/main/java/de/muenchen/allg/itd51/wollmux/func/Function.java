/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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

import java.util.Collection;

/**
 * Eine Funktion, die einen Wert in Abhängigkeit von Parametern berechnet.
 */
public interface Function
{
  /**
   * Liefert die Namen der Parameter, die die Funktion erwartet.
   * Die Reihenfolge ist undefiniert. Es kann kein Name mehrfach vorkommen.
   */
  public String[] parameters();

  /**
   * Zu set werden die Namen aller Funktionsdialoge hinzugefügt, die diese
   * Funktion referenziert.
   */
  public void getFunctionDialogReferences(Collection<String> set);

  /**
   * Ruft die Funktion mit Argumenten aus parameters auf und liefert das
   * Funktionsergebnis als String. Falls es sich um einen booleschen Wert
   * handelt, wird der String "true" oder "false" zurückgeliefert.
   * Falls während der Ausführung ein Fehler auftritt, wird möglicherweise (dies
   * hängt von der Funktion ab) das String-Objekt
   * {@link FunctionLibrary#ERROR} (== vergleichbar) zurückgeliefert.
   * @param parameters sollte zu jedem der von {@link #parameters()} gelieferten
   *        Namen einen String-Wert enthalten.
   */
  public String getString(Values parameters);

   /**
   * Ruft die Funktion mit Argumenten aus parameters auf und liefert das
   * Funktionsergebnis als boolean. Falls der Wert seiner Natur nach ein
   * String ist, so wird true geliefert, falls er (ohne Berücksichtigung von
   * Groß-/Kleinschreibung) der Zeichenkette "true" entspricht.
   * Falls während der Ausführung ein Fehler auftritt wird false zurückgeliefert.
   * @param parameters sollte zu jedem der von {@link #parameters()} gelieferten
   *        Namen einen String-Wert enthalten.
   */
  public boolean getBoolean(Values parameters);

}
