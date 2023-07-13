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
package org.libreoffice.lots.db.mock;

import java.util.HashMap;
import java.util.Map;

import org.libreoffice.lots.db.ColumnNotFoundException;
import org.libreoffice.lots.db.Dataset;

public class MockDataset implements Dataset
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
    throw new ColumnNotFoundException("");
  }

  @Override
  public String getKey()
  {
    return key;
  }

}
