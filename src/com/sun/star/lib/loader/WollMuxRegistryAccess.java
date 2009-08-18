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
 * 18.08.2009 | BED | C_PROGRAMME_WOLLMUX_WOLLMUX_CONF wieder in WollMuxFiles;
 *            |     | Dokumentation; zwei getrennte Methoden für HKCU und HKLM
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */

// Leider ist WinRegKey package protected, also müssen wir uns in diese Package einklinken.
package com.sun.star.lib.loader;

public class WollMuxRegistryAccess
{
  /**
   * Der Pfad (ohne Wurzel wie HKCU oder HKLM) zu dem Registrierungsschlüssel, unter
   * dem der WollMux seine Registry-Werte speichert
   */
  private final static String WOLLMUX_KEY = "Software\\WollMux";

  /**
   * Der Name des String-Wertes, unter dem der WollMux in der Registry den Ort der
   * wollmux.conf speichert
   */
  private final static String WOLLMUX_CONF_PATH_VALUE_NAME = "ConfigPath";

  /**
   * Versucht den Wert {@link #WOLLMUX_CONF_PATH_VALUE_NAME} des
   * Registrierungsschlüssels {@link #WOLLMUX_KEY} unter der Wurzel
   * "HKEY_CURRENT_USER" in der Windows-Registry auszulesen und gibt die in diesem
   * Wert gespeicherten String-Daten zurück.
   * 
   * @return die String-Daten, die im Wert {@link #WOLLMUX_CONF_PATH_VALUE_NAME} des
   *         Schlüssels HKEY_CURRENT_USER\{@link #WOLLMUX_KEY} in der Registry
   *         liegen
   * @throws WollMuxRegistryAccessException
   *           wenn der Wert nicht gefunden wurde oder ein sonstiger Fehler beim
   *           Lesen der Windows-Registry auftrat. Wird diese Methode unter Linux
   *           aufgerufen, sollte immer dieser Fehler geworfen werden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static String getUserWollMuxConfPath()
      throws WollMuxRegistryAccessException
  {
    String path = null;
    try
    {
      WinRegKey key = new WinRegKey("HKEY_CURRENT_USER", WOLLMUX_KEY);
      path = key.getStringValue(WOLLMUX_CONF_PATH_VALUE_NAME);
    }
    // Wir fangen Throwable statt Exception, da unter Linux von WinRegKey ein
    // UnsatisfiedLinkError geworfen wird
    catch (Throwable e)
    {
      throw new WollMuxRegistryAccessException(
        "Fehler beim Lesen der Windows-Registry: " + e.getLocalizedMessage(), e);
    }

    return path;

  }

  /**
   * Versucht den Wert {@link #WOLLMUX_CONF_PATH_VALUE_NAME} des
   * Registrierungsschlüssels {@link #WOLLMUX_KEY} unter der Wurzel
   * "HKEY_LOCAL_MACHINE" in der Windows-Registry auszulesen und gibt die in diesem
   * Wert gespeicherten String-Daten zurück.
   * 
   * @return die String-Daten, die im Wert {@link #WOLLMUX_CONF_PATH_VALUE_NAME} des
   *         Schlüssels HKEY_LOCAL_MACHINE\{@link #WOLLMUX_KEY} in der Registry
   *         liegen
   * @throws WollMuxRegistryAccessException
   *           wenn der Wert nicht gefunden wurde oder ein sonstiger Fehler beim
   *           Lesen der Windows-Registry auftrat. Wird diese Methode unter Linux
   *           aufgerufen, sollte immer dieser Fehler geworfen werden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static String getSharedWollMuxConfPath()
      throws WollMuxRegistryAccessException
  {
    String path = null;
    try
    {
      WinRegKey key = new WinRegKey("HKEY_LOCAL_MACHINE", WOLLMUX_KEY);
      path = key.getStringValue(WOLLMUX_CONF_PATH_VALUE_NAME);
    }
    // Wir fangen Throwable statt Exception, da unter Linux von WinRegKey ein
    // UnsatisfiedLinkError geworfen wird
    catch (Throwable e)
    {
      throw new WollMuxRegistryAccessException(
        "Fehler beim Lesen der Windows-Registry: " + e.getLocalizedMessage(), e);
    }

    return path;
  }
}
