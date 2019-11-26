/*
 * Dateiname: EmptyDatasource.java
 * Projekt  : WollMux
 * Funktion : Eine Datenquelle, die keine Datensätze enthält.
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
 * 28.10.2005 | BNK | Erstellung
 * 28.10.2005 | BNK | +getName()
 * 31.10.2005 | BNK | +find()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

/**
 * Eine Datenquelle, die keine Datensätze enthält.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class EmptyDatasource implements Datasource
{
  private static QueryResults emptyResults =
    new QueryResultsList(new Vector<Dataset>(0));

  private List<String> schema;

  private String name;

  public EmptyDatasource(List<String> schema, String name)
  {
    this.schema = schema;
    this.name = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection,
   *      long)
   */
  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout)
  {
    return emptyResults;
  }

  @Override
  public QueryResults getContents(long timeout) throws TimeoutException
  {
    return emptyResults;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public QueryResults find(List<QueryPart> query, long timeout)
      throws TimeoutException
  {
    return emptyResults;
  }
}
