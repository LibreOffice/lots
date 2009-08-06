/*
 * Dateiname: WollMuxRegistryAccess.java
 * Projekt  : WollMux
 * Funktion : Liest WollMux-Config-Daten aus der Windows Registry
 * 
 * Copyright (c) 2009 Landeshauptstadt München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), 
 * version 1.0.
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
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */

// Leider ist WinRegKey package protected, also müssen wir uns in diese Package einklinken.
package com.sun.star.lib.loader;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;

public class WollMuxRegistryAccess
{
  private final static String WOLLMUX_CONF_DIR_KEY = "Software\\WollMux\\ConfigPath";

  private static final String C_PROGRAMME_WOLLMUX_WOLLMUX_CONF =
    "C:\\Programme\\wollmux\\wollmux.conf";

  public static String getWollMuxConfDir()
  {
    String path = C_PROGRAMME_WOLLMUX_WOLLMUX_CONF;
    try
    {
      WinRegKey key = new WinRegKey("HKEY_CURRENT_USER", WOLLMUX_CONF_DIR_KEY);
      path = key.getStringValue(""); // default
    }
    catch (Exception e)
    {
      try
      {
        WinRegKey key = new WinRegKey("HKEY_LOCAL_MACHINE", WOLLMUX_CONF_DIR_KEY);
        path = key.getStringValue(""); // default
      }
      catch (Exception x)
      {
        Logger.error(L.m("Fehler beim Lesen der Windows-Registry"), x);
      }
    }

    return path;

  }
}
