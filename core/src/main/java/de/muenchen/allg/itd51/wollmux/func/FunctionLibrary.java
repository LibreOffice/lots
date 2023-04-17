/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
 * A library of named functions
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionLibrary implements Iterable<Function>
{
  public static final String ERROR = L.m("!¤£!INCORRECT DATA!¤£!");
  
  private Map<String, Function> mapIdToFunction;

  private FunctionLibrary baselib;

  /**
   * Creates an empty function library.
   */
  public FunctionLibrary()
  {
    this(null);
  }

  /**
   * Generates a function library that references baselib (not copied!).
   * baselib is always queried if the function library itself does not
   * Contains function of the corresponding name. baselib may be null.
   *
   * @param baselib
   */
  public FunctionLibrary(FunctionLibrary baselib)
  {
    this(baselib, false);
  }

  /**
   * Generates a function library that references baselib (not copied!).
   * baselib is always queried if the function library itself does not
   * Contains function of the corresponding name. baselib may be null.
   *
   * @param ordered
   *          If true, the iterator of this function library returns the
   *          Features in insertion order. Otherwise they will be indefinite
   *          Order delivered. It should be obvious that
   *          ordered==true comes with increased overhead.
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
   * Adds func to this function library under the name funcName. One
   * already existing function with this name will be replaced.
   */
  public void add(String funcName, Function func)
  {
    if (func == null || funcName == null)
      throw new NullPointerException(
        L.m("Neither function name nor function may be null"));
    mapIdToFunction.put(funcName, func);
  }

  /**
   * Returns the function named funcName or null if no function is included
   * known by that name. Was the function library with a reference to
   * initializes another function library, it will be queried if the
   * Function library itself does not know a function of the corresponding name.
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
   * Tried all functions named funcName from this and possibly chained
   * Remove function libraries.
   *
   * @return true if after executing the {@link #hasFunction(String)} command for
   *         funcName returns false, otherwise false. I.e. true is returned,
   *         if all functions could be removed. If false
   *         is returned, some may have been, but definitely not all
   *         Features removed. If no function funcName
   *         was present, true is also returned.
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
   * Returns true if this function library knows a function named funcName.
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
   * Returns the names of all functions available through this function library
   * are.
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
