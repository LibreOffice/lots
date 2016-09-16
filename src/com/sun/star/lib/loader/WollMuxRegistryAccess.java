/*
 * Dateiname: WollMuxRegistryAccess.java
 * Projekt  : WollMux
 * Funktion : Liest WollMux-Config-Daten aus der Windows Registry
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 06.08.2009 | BNK | Erstellung
 * 18.08.2009 | BED | C_PROGRAMME_WOLLMUX_WOLLMUX_CONF wieder in WollMuxFiles;
 *            |     | Dokumentation; zwei getrennte Methoden für HKCU und HKLM
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */

// Leider ist WinRegKey package protected, also müssen wir uns in diese Package einklinken.
package com.sun.star.lib.loader;

import de.muenchen.allg.itd51.wollmux.core.util.L;

public class WollMuxRegistryAccess
{
  /**
   * Liefert die String-Daten des Werts namens valueName des Registrierungsschlüssels
   * keyName unter der Wurzel root in der Windows-Registry zurück.
   * 
   * @throws WollMuxRegistryAccessException
   *           wenn der Wert nicht gefunden wurde oder ein sonstiger Fehler beim
   *           Lesen der Windows-Registry auftrat. Wird diese Methode unter Linux
   *           aufgerufen, sollte immer dieser Fehler geworfen werden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static String getStringValueFromRegistry(String root, String keyName,
      String valueName) throws WollMuxRegistryAccessException
  {
    String path = null;
    try
    {
      WinRegKey key = new WinRegKey(root, keyName);
      path = key.getStringValue(valueName);
    }
    // Wir fangen Throwable statt Exception, da unter Linux von WinRegKey ein
    // UnsatisfiedLinkError geworfen wird
    catch (Throwable e)
    {
      throw new WollMuxRegistryAccessException(L.m(
        "Fehler beim Lesen von Wert %3 aus %1\\%2 aus der Windows-Registry: %4",
        root, keyName, valueName, e.getLocalizedMessage()), e);
    }

    return path;

  }
}
