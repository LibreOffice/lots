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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.collections4.CollectionUtils;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.util.L;

/**
 * Datasource that represents the union of 2 datasources
 */
public class UnionDatasource extends Datasource
{
  private Datasource source1;

  private Datasource source2;

  private String source1Name;

  private String source2Name;

  private List<String> schema;

  private String name;

  /**
   * Creates a new UnionDatasource.
   *
   * @param nameToDatasource
   *          contains all up to the time this UnionDatasource was defined
   *          Already fully instantiated data sources.
   * @param sourceDesc
   *          the "DataSource" node, which contains the description of this UnionDatasource
   *          contains.
   * @param context
   *          the context relative to which URLs should be resolved (currently not
   *          used).
   */
  public UnionDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc, URL context)
  {
    name = parseConfig(sourceDesc, "NAME", () -> L.m("NAME of data source is missing"));
    source1Name = parseConfig(sourceDesc, "SOURCE1", () ->
      L.m("\"{0}\" of data source \"{1}\" is missing.", "SOURCE1", name));
    source2Name = parseConfig(sourceDesc, "SOURCE2", () ->
      L.m("\"{0}\" of data source \"{1}\" is missing.", "SOURCE2", name));

    source1 = nameToDatasource.get(source1Name);
    source2 = nameToDatasource.get(source2Name);

    if (source1 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source1Name));

    if (source2 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source2Name));

    /*
     * Note: The following condition is "unnecessarily" strict, but around them
     * softening it (e.g. overall schema is a union of schemas) would be it
     * required to implement a dataset wrapper that ensures that
     * all datasets returned in QueryResults -the same schema
     * have. As long as there is no obvious need for it, I'll save it
     * Expense.
     */
    List<String> schema1 = source1.getSchema();
    List<String> schema2 = source2.getSchema();
    if (!schema1.containsAll(schema2) || !schema2.containsAll(schema1))
    {
      Set<String> difference1 = new HashSet<>(schema1);
      difference1.removeAll(schema2);
      Set<String> difference2 = new HashSet<>(schema2);
      difference2.removeAll(schema1);
      StringBuilder buf1 = new StringBuilder();
      Iterator<String> iter = difference1.iterator();
      while (iter.hasNext())
      {
        buf1.append(iter.next());
        if (iter.hasNext()) {
          buf1.append(", ");
        }
      }
      StringBuilder buf2 = new StringBuilder();
      iter = difference2.iterator();
      while (iter.hasNext())
      {
        buf2.append(iter.next());
        if (iter.hasNext()) {
          buf2.append(", ");
        }
      }
      throw new ConfigurationErrorException(
        L.m(
          "In datasource \"{0}\" columns: {1} are missing and in datasource \"{2}\" columns: {3} are missing",
          source1Name, buf2, source2Name, buf1));
    }

    schema = new ArrayList<>(schema1);
  }

  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    Collection<Dataset> result = CollectionUtils.union(source1.getDatasetsByKey(keys), source2.getDatasetsByKey(keys));
    return new QueryResultsList(result.iterator(), 0);
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    Collection<Dataset> result = CollectionUtils.union(source1.find(query), source2.find(query));
    return new QueryResultsList(result.iterator(), 0);
  }

  @Override
  public String getName()
  {
    return name;
  }

}
