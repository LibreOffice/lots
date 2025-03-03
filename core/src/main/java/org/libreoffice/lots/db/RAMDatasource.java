/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

/**
 * Superclass for data sources that keep their data entirely in memory
 */
public class RAMDatasource extends Datasource
{
  /**
   * The schema of this data source.
   */
  private List<String> schema;

  /**
   * List of all datasets stored in this datasource.
   */
  private List<Dataset> data;

  /**
   * The name of this data source.
   */
  private String name;

  /**
   * Creates a new RAMDatasource with the name 'name'. 'data' and 'schema' are included as references, not copied.
   *
   * @param name
   *          the name of the data source
   * @param schema
   *          the schema of the data source
   * @param data
   *          the data source records
   */
  public RAMDatasource(String name, List<String> schema, List<Dataset> data)
  {
    init(name, schema, data);
  }

  /**
   * Creates an uninitialized RAMDatasource. A derived class that uses this constructor should call 'init()'
   * to perform the necessary initializations.
   */
  protected RAMDatasource()
  {
  }

  /**
   * Performs the initialization actions of the constructor with the same parameters.
   * This method should be used by derived classes when they use the constructor without arguments.
   */
  protected void init(String name, List<String> schema, List<Dataset> data)
  {
    this.schema = schema;
    this.data = data;
    this.name = name;
  }

  @Override
  public List<String> getSchema()
  {
    return new ArrayList<>(schema);
  }

  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    List<Dataset> res = new ArrayList<>();
    Iterator<Dataset> iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset ds = iter.next();
      if (keys.contains(ds.getKey()))
      {
        res.add(ds);
      }
    }

    return new QueryResultsList(res);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#find(java.util.List, long)
   */
  @Override
  public QueryResults find(List<QueryPart> query)
  {
    if (query.isEmpty())
    {
      return new QueryResultsList(new Vector<Dataset>(0));
    }

    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);

    List<Dataset> results = new ArrayList<>();

    data.forEach(ds -> {
      if (pred.test(ds))
      {
        results.add(ds);
      }
    });

    return new QueryResultsList(results);
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<>(data));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#getName()
   */
  @Override
  public String getName()
  {
    return name;
  }

}
