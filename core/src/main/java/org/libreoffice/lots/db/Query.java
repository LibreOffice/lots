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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Stores the name of a data source and a search query on it.
 */
public class Query implements Iterable<QueryPart>
{
  /**
   * List of QueryParts that describe the actual query.
   */
  private List<QueryPart> listOfQueryParts;

  /**
   * The name of the data source to search on.
   */
  private String dbName;

  /**
   * Creates a new query.
   * @param dbName the name of the data source on which the search should take place.
   * @param listOfQueryParts List of QueryParts that contain the actual query
   *        Describe. DANGER! Will be included as a reference.
   */
  public Query(String dbName, List<QueryPart> listOfQueryParts)
  {
    this.listOfQueryParts = listOfQueryParts;
    this.dbName = dbName;
  }

  /**
   * Provides the name of the data source on which the search should take place.
   */
  public String getDatasourceName() {return dbName;}

  /**
   * Iterates over the QueryParts that make up this query.
   */
  @Override
  public Iterator<QueryPart> iterator() {return listOfQueryParts.iterator();}

  /**
   * Returns the list of QueryParts that define this search query.
   */
  public List<QueryPart> getQueryParts() {return new Vector<>(listOfQueryParts);}

  public int numberOfQueryParts() { return listOfQueryParts.size();}
}
