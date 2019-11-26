/*
 * Dateiname: FunctionLibrary.java
 * Projekt  : WollMux
 * Funktion : Eine Bibliothek von benannten Functions
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
 * 08.05.2006 | BNK | Fertig implementiert.
 * 26.09.2006 | BNK | +hasFunction()
 * 27.09.2006 | BNK | +getFunctionNames()
 * 15.11.2007 | BNK | +remove()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Eine Bibliothek von benannten Functions
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionLibrary implements Iterable<Function>
{
  public static final String ERROR = L.m("!¤£!FEHLERHAFTE DATEN!¤£!");
  
  private Map<String, Function> mapIdToFunction;

  private FunctionLibrary baselib;

  /**
   * Erzeugt eine leere Funktionsbibliothek.
   */
  public FunctionLibrary()
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
  public FunctionLibrary(FunctionLibrary baselib)
  {
    this(baselib, false);
  }

  /**
   * Erzeugt eine Funktionsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Funktionsbibliothek selbst keine
   * Funktion des entsprechenden Namens enthält. baselib darf null sein.
   *
   * @param ordered
   *          Falls true liefert der Iterator dieser Funktionsbibliothek die
   *          Funktionen in Einfügereihenfolge. Ansonsten werden sie in unbestimmter
   *          Reihenfolge geliefert. Es sollte offensichtlich sein, dass
   *          ordered==true mit einem erhöhten Overhead verbunden ist.
   */
  public FunctionLibrary(FunctionLibrary baselib, boolean ordered)
  {
    if (ordered)
      mapIdToFunction = new LinkedHashMap<>();
    else
      mapIdToFunction = new HashMap<>();
    this.baselib = baselib;
  }

  /**
   * Fügt func dieser Funktionsbibliothek unter dem Namen funcName hinzu. Eine
   * bereits existierende Funktion mit diesem Namen wird dabei ersetzt.
   */
  public void add(String funcName, Function func)
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
   */
  public Function get(String funcName)
  {
    Function func = mapIdToFunction.get(funcName);
    if (func == null && baselib != null) {
      func = baselib.get(funcName);
    }
    return func;
  }

  /**
   * Versucht, alle Funktionen namens funcName aus dieser und evtl, verketteter
   * Funktionsbibliotheken zu entfernen.
   *
   * @return true, falls nach Ausführung des Befehls {@link #hasFunction(String)} für
   *         funcName false zurückliefert, false sonst. D.h. true wird geliefert,
   *         wenn alle Funktionen entfernt werden konnten. Falls false
   *         zurückgeliefert wird, wurden evtl. manche, aber definitiv nicht alle
   *         Funktionen entfernt. Falls von vorneherein keine Funktion funcName
   *         vorhanden war, wird auch true geliefert.
   */
  public boolean remove(String funcName)
  {
    mapIdToFunction.remove(funcName);
    if (baselib != null) {
      return baselib.remove(funcName);
    }
    return true;
  }

  /**
   * Liefert true wenn diese Funktionsbibliothek eine Funktion namens funcName kennt.
   */
  public boolean hasFunction(String funcName)
  {
    if (mapIdToFunction.containsKey(funcName)) {
      return true;
    }
    if (baselib != null) {
      return baselib.hasFunction(funcName);
    }
    return false;
  }

  /**
   * Liefert die Namen aller Funktionen, die über diese Funktionsbibliothek verfügbar
   * sind.
   */
  public Set<String> getFunctionNames()
  {
    Set<String> names = new HashSet<>(mapIdToFunction.keySet());
    if (baselib != null) {
      names.addAll(baselib.getFunctionNames());
    }
    return names;
  }

  @Override
  public Iterator<Function> iterator()
  {
    return new IteratorWrapper(mapIdToFunction.values().iterator());
  }

  private static class IteratorWrapper implements Iterator<Function>
  {
    private Iterator<Function> iter;

    public IteratorWrapper(Iterator<Function> iter)
    {
      this.iter = iter;
    }

    @Override
    public boolean hasNext()
    {
      return iter.hasNext();
    }

    @Override
    public Function next()
    {
      return iter.next();
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
