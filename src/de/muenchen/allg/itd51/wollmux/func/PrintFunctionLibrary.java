/*
 * Dateiname: PrintFunctionLibrary.java
 * Projekt  : WollMux
 * Funktion : Eine Bibliothek von benannten Druckfunktionen
 * 
 * Copyright (c) 2008 Landeshauptstadt München
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
 * 19.09.2006 | LUT | Erstellung.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.L;

/**
 * Eine Bibliothek von benannten PrintFunctions
 * 
 * @author christoph.lutz
 */
public class PrintFunctionLibrary
{
  private Map<String, PrintFunction> mapIdToFunction;

  private PrintFunctionLibrary baselib;

  /**
   * Erzeugt eine leere Funktionsbibliothek.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public PrintFunctionLibrary()
  {
    this(null);
  }

  /**
   * Erzeugt eine Funktionsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Funktionsbibliothek selbst keine
   * Funktion des entsprechenden Namens enthält. baselib darf null sein.
   * 
   * @param baselib
   */
  public PrintFunctionLibrary(PrintFunctionLibrary baselib)
  {
    mapIdToFunction = new HashMap<String, PrintFunction>();
    this.baselib = baselib;
  }

  /**
   * Fügt func dieser Funktionsbibliothek unter dem Namen funcName hinzu. Eine
   * bereits existierende Funktion mit diesem Namen wird dabei ersetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(String funcName, PrintFunction func)
  {
    if (func == null || funcName == null)
      throw new NullPointerException(
        L.m("Weder Funktionsname noch Funktion darf null sein"));
    mapIdToFunction.put(funcName, func);
  }

  /**
   * Liefert die Function namens funcName zurück oder null, falls keine Funktion mit
   * diesem Namen bekannt ist. Wurde die Funktionsbibliothek mit einer Referenz auf
   * eine andere Funktionsbibliothek initialisiert, so wird diese befragt, falls die
   * Funktionsbibliothek selbst keine Funktion des entsprechenden Namens kennt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public PrintFunction get(String funcName)
  {
    PrintFunction func = mapIdToFunction.get(funcName);
    if (func == null && baselib != null) func = baselib.get(funcName);
    return func;
  }

  /**
   * Liefert die Namen aller Funktionen, die über diese Funktionsbibliothek verfügbar
   * sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set<String> getFunctionNames()
  {
    Set<String> names = new HashSet<String>(mapIdToFunction.keySet());
    if (baselib != null) names.addAll(baselib.getFunctionNames());
    return names;
  }

}
