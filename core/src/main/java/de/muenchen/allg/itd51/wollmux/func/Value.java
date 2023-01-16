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

/**
 * Ein Wert, der als verschiedene Datentypen abrufbar ist
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Value
{
  /**
   * Der aktuelle Wert als String. Falls es sich um einen booleschen Wert handelt,
   * wird der String "true" oder "false" zurückgeliefert.
   */
  public String getString();

  /**
   * Der aktuelle Wert als boolean. Falls der Wert seiner Natur nach ein String ist,
   * so ist das Ergebnis abhängig von der konkreten Implementierung.
   */
  public boolean getBoolean();
}
