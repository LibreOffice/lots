/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Eine simple Implementierung des Interfaces Dataset.
 */
public class SimpleDataset implements Dataset
{
  private Map<String, String> data;

  private String key;

  /**
   * Erzeugt ein SimpleDataset, das eine Kopie von ds ist. Das erzeugte SimpleDataset
   * ist von schema und ds unabhängig und hält keine Verknüpfungen darauf.
   * 
   * @param schema
   *          enthält die Namen aller zu kopierenden Spalten
   * @param ds
   *          der zu kopierende Datensatz.
   * @throws ColumnNotFoundException
   *           falls eine Spalte aus schema dem Datensatz ds nicht bekannt ist
   */
  public SimpleDataset(Collection<String> schema, Dataset ds)
      throws ColumnNotFoundException
  {
    key = ds.getKey();
    data = new HashMap<>();
    for (String spalte : schema)
    {
      data.put(spalte, ds.get(spalte));
    }
  }

  /**
   * Erzeugt ein SimpleDataset, das den Schlüssel key hat und dessen Daten von der
   * Map data geliefert werden. Das Schema wird implizit durch die Schlüssel
   * bestimmt, die data kennt (d.h. wenn data.containsKey(column), dann ist column im
   * Schema). ACHTUNG! Sowohl schema als auch data werden per Referenz eingebunden.
   */
  public SimpleDataset(String key, Map<String, String> data)
  {
    this.key = key;
    this.data = data;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Dataset#get(java.lang.String)
   */
  @Override
  public String get(String columnName) throws ColumnNotFoundException
  {
    if (!data.containsKey(columnName))
      throw new ColumnNotFoundException(L.m("Datensatz kennt Spalte \"%1\" nicht!",
        columnName));
    return data.get(columnName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Dataset#getKey()
   */
  @Override
  public String getKey()
  {
    return key;
  }
}
