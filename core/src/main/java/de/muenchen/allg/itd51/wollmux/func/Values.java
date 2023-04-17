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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A lot of named {@link de.muenchen.allg.itd51.wollmux.func.Value}s.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Values
{
  /**
   * Returns true if and only if a value with the ID id is present (ATTENTION, with
   * composite functions with BIND gets the bound function underneath
   * Circumstances not an accurate answer here).
   */
  public boolean hasValue(String id);

  /**
   * The current value of the value identified with id as a string. If it's about
   * is a boolean value, the string will be "true" or "false"
   * returned. If there is no value with this id, it will be empty
   * String supplied.
   */
  public String getString(String id);

  /**
   * The current value of the value identified by id as a boolean. If the value
   * is a string by nature, the result is implementation dependent.
   * If there is no value with this id, false is returned.
   */
  public boolean getBoolean(String id);

  /**
   * Dummy class that exposes a Values ​​interface that doesn't have values
   * contains.
   */
  public static class None implements Values
  {
    @Override
    public boolean hasValue(String id)
    {
      return false;
    }

    @Override
    public String getString(String id)
    {
      return "";
    }

    @Override
    public boolean getBoolean(String id)
    {
      return false;
    }
  }

  /**
   * Simple implementation of the values ​​interface in the style of a map.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static class SimpleMap implements Values, Iterable<Map.Entry<String, String>>
  {
    private final HashMap<String, String> values;

    public SimpleMap()
    {
      values = new HashMap<>();
    }

    public SimpleMap(SimpleMap origin)
    {
      this.values = new HashMap<>(origin.values);
    }

    /**
     * Adds the value value identified with id. A value that already exists
     * is replaced. If value==null, then the call is treated as
     * {@link #remove(String)}.
     */
    public void put(String id, String value)
    {
      if (value == null)
        remove(id);
      else
        values.put(id, value);
    }

    /**
     * Removes the value identified by id (if any).
     */
    public void remove(String id)
    {
      values.remove(id);
    }

    /**
     * Adds all values ​​from the other SimpleMap to this one.
     *
     * @param map
     *          The other SimpleMap to take the values ​​from.
     */
    public void putAll(SimpleMap map)
    {
      values.putAll(map.values);
    }

    @Override
    public boolean hasValue(String id)
    {
      return values.containsKey(id);
    }

    @Override
    public String getString(String id)
    {
      String str = values.get(id);
      if (str == null) {
        return "";
      }
      return str;
    }

    @Override
    public boolean getBoolean(String id)
    {
      return "true".equalsIgnoreCase(getString(id));
    }

    @Override
    public Iterator<Entry<String, String>> iterator()
    {
      return values.entrySet().iterator();
    }
  }
}
