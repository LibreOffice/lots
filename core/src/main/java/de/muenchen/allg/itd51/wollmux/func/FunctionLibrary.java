/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Eine Bibliothek von benannten Functions
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionLibrary implements Iterable<Function>
{
  public static final String ERROR = L.m("!¤£!INCORRECT DATA!¤£!");
  
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
        L.m("Neither function name nor function may be null"));
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
