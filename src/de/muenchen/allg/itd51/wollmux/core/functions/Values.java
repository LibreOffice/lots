/*
 * Dateiname: Values.java
 * Projekt  : WollMux
 * Funktion : Eine Menge benannter Values.
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
 * 04.05.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.HashMap;

/**
 * Eine Menge benannter {@link de.muenchen.allg.itd51.wollmux.core.functions.Value}s.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Values
{
  /**
   * Liefert true genau dann wenn ein Wert mit der ID id vorhanden ist (ACHTUNG, bei
   * mit BIND zusammengesetzten Funktionen bekommt die gebundene Funktion unter
   * Umständen hier keine akkurate Antwort).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasValue(String id);

  /**
   * Der aktuelle Wert des mit id identifizierten Values als String. Falls es sich um
   * einen booleschen Wert handelt, wird der String "true" oder "false"
   * zurückgeliefert. Falls kein Wert mit dieser id vorhanden ist wird der leere
   * String geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString(String id);

  /**
   * Der aktuelle Wert des mit id identifizierten Values als boolean. Falls der Wert
   * seiner Natur nach ein String ist, so ist das Ergebnis implementierungsabhängig.
   * Falls kein Wert mit dieser id vorhanden ist wird false geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getBoolean(String id);

  /**
   * Dummy-Klasse, die ein Values-Interface zur Verfügung stellt, das keine Werte
   * enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static class None implements Values
  {
    public boolean hasValue(String id)
    {
      return false;
    }

    public String getString(String id)
    {
      return "";
    }

    public boolean getBoolean(String id)
    {
      return false;
    }
  }

  /**
   * Simple Implementierung des Values-Interfaces in der Art einer Map.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static class SimpleMap implements Values
  {
    private HashMap<String, String> values = new HashMap<String, String>();

    /**
     * Fügt den Wert value hinzu, identifiziert mit id. Ein bereits vorhandener Wert
     * wird ersetzt. Falls value==null, so wird der Aufruf behandelt wie
     * {@link #remove(String)}.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void put(String id, String value)
    {
      if (value == null)
        remove(id);
      else
        values.put(id, value);
    }

    /**
     * Entfernt den Wert, der durch id identifiziert wird (falls vorhanden).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void remove(String id)
    {
      values.remove(id);
    }

    public boolean hasValue(String id)
    {
      return values.containsKey(id);
    }

    public String getString(String id)
    {
      String str = values.get(id);
      if (str == null) return "";
      return str;
    }

    public boolean getBoolean(String id)
    {
      return getString(id).equalsIgnoreCase("true");
    }
  }
}
