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
package org.libreoffice.lots.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.libreoffice.lots.util.L;

/**
 * A simple implementation of the Dataset interface.
 */
public class SimpleDataset implements Dataset {
  private Map<String, String> data;

  private String key;

  /**
   * Creates a SimpleDataset that is a copy of ds. This created SimpleDataset
   * is independent of schema and ds and does not hold links to them.
   *
   * @param schema
   *               contains the names of all columns to be copied
   * @param ds
   *               the data record to be copied.
   * @throws ColumnNotFoundException
   *                                 if a column from schema is not known to the
   *                                 data set ds
   */
  public SimpleDataset(Collection<String> schema, Dataset ds)
      throws ColumnNotFoundException {
    key = ds.getKey();
    data = new HashMap<>();
    for (String spalte : schema) {
      data.put(spalte, ds.get(spalte));
    }
  }

  /**
   * Creates a SimpleDataset that has the key key and whose data is from the
   * Map data will be supplied. The schema is implicit through the key
   * determines which data knows (i.e. if data.containsKey(column), then column is
   * in
   * Scheme). DANGER! Both schema and data are included by reference.
   */
  public SimpleDataset(String key, Map<String, String> data) {
    this.key = key;
    this.data = data;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Dataset#get(java.lang.String)
   */
  @Override
  public String get(String columnName) throws ColumnNotFoundException {
    if (!data.containsKey(columnName))
      throw new ColumnNotFoundException(L.m("Data set does not know column \"{0}\"",
          columnName));
    return data.get(columnName);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Dataset#getKey()
   */
  @Override
  public String getKey() {
    return key;
  }
}
