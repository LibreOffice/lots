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
package org.libreoffice.lots.db.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.libreoffice.lots.db.ColumnNotFoundException;
import org.libreoffice.lots.db.Dataset;
import org.libreoffice.lots.db.Datasource;
import org.libreoffice.lots.db.QueryPart;
import org.libreoffice.lots.db.QueryResults;

public class MockDatasource extends Datasource
{

  private List<Dataset> datasets;
  private String name;
  private List<String> schema;

  public MockDatasource(String name, List<String> schema, List<Dataset> datasets)
  {
    this.name = name;
    this.datasets = datasets;
    this.schema = schema;
  }

  public MockDatasource()
  {
    this("mock", List.of("column"), List.of(new MockDataset(), new MockDataset("ds2", "column", "value2")));
  }

  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    return new MockQueryResults(datasets.stream().filter(ds -> keys.contains(ds.getKey())).toArray(Dataset[]::new));
  }

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    List<Dataset> found = new ArrayList<>();
    for (Dataset ds : datasets)
    {
      boolean match = true;
      for (QueryPart part : query)
      {
        try
        {
          match = match && ds.get(part.getColumnName()).equals(part.getSearchString());
        } catch (ColumnNotFoundException ex)
        {
          match = false;
          break;
        }
      }
      if (match)
      {
        found.add(ds);
      }
    }
    return new MockQueryResults(found.toArray(Dataset[]::new));
  }

  @Override
  public QueryResults getContents()
  {
    return new MockQueryResults(datasets.toArray(Dataset[]::new));
  }

  @Override
  public String getName()
  {
    return name;
  }

}
