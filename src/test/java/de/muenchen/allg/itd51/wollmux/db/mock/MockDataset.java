/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.db.mock;

import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.NoBackingStoreException;

public class MockDataset implements DJDataset
{
  private String key;
  private Map<String, String> data;

  public MockDataset(String key, String column, String value)
  {
    this.key = key;
    data = new HashMap<>();
    data.put(column, value);
  }

  public MockDataset(String key, Map<String, String> data)
  {
    this.key = key;
    this.data = data;
  }

  public MockDataset()
  {
    this("ds", "column", "value");
  }

  @Override
  public String get(String columnName) throws ColumnNotFoundException
  {
    if (data.containsKey(columnName))
    {
      return data.get(columnName);
    }
    throw new ColumnNotFoundException();
  }

  @Override
  public String getKey()
  {
    return key;
  }

  @Override
  public void set(String columnName, String newValue) throws ColumnNotFoundException
  {
  }

  @Override
  public boolean hasLocalOverride(String columnName) throws ColumnNotFoundException
  {
    return false;
  }

  @Override
  public boolean hasBackingStore()
  {
    return false;
  }

  @Override
  public boolean isFromLOS()
  {
    return false;
  }

  @Override
  public boolean isSelectedDataset()
  {
    return false;
  }

  @Override
  public void select()
  {
  }

  @Override
  public void discardLocalOverride(String columnName) throws ColumnNotFoundException, NoBackingStoreException
  {
  }

  @Override
  public DJDataset copy()
  {
    return null;
  }

  @Override
  public void remove()
  {
  }

}
