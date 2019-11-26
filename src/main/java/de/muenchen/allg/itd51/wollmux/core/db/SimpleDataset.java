/*
 * Dateiname: SimpleDataset.java
 * Projekt  : WollMux
 * Funktion : Eine simple Implementierung des Interfaces Dataset.
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
 * 06.12.2005 | BNK | Erstellung
 * 30.05.2006 | BNK | +SimpleDataset(String key, Map data)
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.util.L;

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
