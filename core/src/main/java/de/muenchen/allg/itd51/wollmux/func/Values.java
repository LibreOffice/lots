/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
 * Plenty of known {@link de.muenchen.allg.itd51.wollmux.func.Value}s.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Values
{
  /**
   * Deliver exact true then, if a value with the ID id exists (WARNING, the bound
   * function get possibly no accurate answer here for functions
   * put together with BIND).
   */
  public boolean hasValue(String id);

  /**
   * The current value of the value identified by id as String. If it is
   * a bolean value, the String 'true' or 'false' will be
   * delivered. If no value with this id is available an empty
   * String will be delivered.
   */
  public String getString(String id);

  /**
   * The current value of the value identified by id as boolean. If the value
   * by its nature is a String, the result is implementation dependent.
   * If no value with this id is available false will be delivered.
   */
  public boolean getBoolean(String id);

  /**
   * Dummy class, that provides a values interface, which contains no values.
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
   * Simple implementation of the values interface in the way of a map.
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
     * Add the value identified by idF. An allready existing value will
     * be replaced. If value==null, the call will be treated like
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
     * Removes the value, which was identified by id (if existing).
     */
    public void remove(String id)
    {
      values.remove(id);
    }

    /**
     * Add all values from the other SimpleMap to this one.
     *
     * @param map
     *          The other SimpleMap, from which the values should be taken.
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
