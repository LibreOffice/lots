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
import java.util.List;
import java.util.Vector;

/**
 * A data source that contains no records.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class EmptyDatasource extends Datasource
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
   * @see org.libreoffice.lots.db.Datasource#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#getDatasetsByKey(java.util.Collection,
   *      long)
   */
  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    return emptyResults;
  }

  @Override
  public QueryResults getContents()
  {
    return emptyResults;
  }

  @Override
  public String getName()
  {
    return name;
  }

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    return emptyResults;
  }
}
