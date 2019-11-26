/*
* Dateiname: Function.java
* Projekt  : WollMux
* Funktion : Eine Funktion, die einen Wert in Abhängigkeit von Parametern berechnet.
*
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 03.05.2006 | BNK | Erstellung
* 31.05.2006 | BNK | +getFunctionDialogReferences()
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
*
*/
package de.muenchen.allg.itd51.wollmux.core.functions;

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
