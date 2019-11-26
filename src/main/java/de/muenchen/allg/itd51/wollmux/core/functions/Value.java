/*
 * Dateiname: Value.java
 * Projekt  : WollMux
 * Funktion : Ein Wert, der als verschiedene Datentypen abrufbar ist.
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
 * 02.02.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.core.functions;

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
