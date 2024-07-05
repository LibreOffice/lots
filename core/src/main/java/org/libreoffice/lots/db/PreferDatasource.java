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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.util.L;

/**
 * Datasource, the data from one data source A from files from another data source B
 * can be concealed. The way this works is that queries are only sent to data source A
 * are set and then all result data records are checked to see whether a
 * Record (or multiple records) with the same key is in data source B.
 * If so, only the records from data source are used for this key
 * B returned.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PreferDatasource extends Datasource
{
  private Datasource source1;

  private Datasource source2;

  private String source1Name;

  private String source2Name;

  private List<String> schema;

  private String name;

  /**
   * Creates a new PreferDatasourcee.
   *
   * @param nameToDatasource
   *          Contains all data sources that have already been fully instantiated
   *          up to the point of defining this PreferDatasource.
   * @param sourceDesc
   *          the "DataSource" node that contains the description of this PreferDatasource.
   * @param context
   *         the context in which the URLs should be resolved (not currently used).
   */
  public PreferDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc, URL context)
  {
    name = parseConfig(sourceDesc, "NAME", () -> L.m("NAME of data source is missing"));
    source1Name = parseConfig(sourceDesc, "SOURCE", () -> L.m("SOURCE of data source {0} is missing", name));
    source2Name = parseConfig(sourceDesc, "OVER", () -> L.m("OVER-Specification of data source {0} is missing", name));

    source1 = nameToDatasource.get(source1Name);
    source2 = nameToDatasource.get(source2Name);

    if (source1 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name));

    if (source2 == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, source2Name));

    /*
     * The following condition is "unnecessarily" strict, but to relax it (e.g., assuming that the overall
     * schema is always the schema of the preferred data source),
     * it would be necessary to implement a dataset wrapper that ensures that all datasets returned
     * in QueryResults have the same schema. As long as there is no apparent need for this, I will save myself this effort.
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
      throw new ConfigurationErrorException(L.m(
        "In datasource \"{0}\" columns: {1} are missing", source1Name, buf2)
        + L.m(" and ")
        + L.m("in datasource \"{0}\" columns: {1} are missing", source2Name, buf1));
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
    return new QueryResultsOverride(source2.getDatasetsByKey(keys), source1.getDatasetsByKey(keys),
        source1);
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    return new QueryResultsOverride(source2.find(query), source1.find(query), source1);
  }

  @Override
  public String getName()
  {
    return name;
  }

  private static class QueryResultsOverride implements QueryResults
  {
    private int size;

    private Set<String> keyDenylist = new HashSet<>();

    private QueryResults overrideResults;

    private QueryResults results;

    public QueryResultsOverride(QueryResults results, QueryResults overrideResults,
        Datasource override)
    {
      this.overrideResults = overrideResults;
      this.results = results;
      this.size = results.size();

      Map<String, int[]> keyToCount = new HashMap<>(); // of int[]

      Iterator<Dataset> iter = results.iterator();
      while (iter.hasNext())
      {
        Dataset ds = iter.next();
        String key = ds.getKey();
        if (!keyToCount.containsKey(key)) {
          keyToCount.put(key, new int[] { 0 });
        }
        int[] count = keyToCount.get(key);
        ++count[0];
      }

      /**
       * Records for which a correction record exists but do not appear in overrideResults 
       * (because the correction has made the search condition no longer valid) must 
       * also be added to the denylist with their key. Therefore, we need to search for these records.
       */
      QueryResults denylistResults =
          override.getDatasetsByKey(keyToCount.keySet());

      size += overrideResults.size();

      QueryResults[] oResults = new QueryResults[] {
        overrideResults, denylistResults };

      for (int i = 0; i < oResults.length; ++i)
      {
        iter = oResults[i].iterator();
        while (iter.hasNext())
        {
          Dataset ds = iter.next();
          String key = ds.getKey();

          int[] count = keyToCount.get(key);
          if (count != null)
          {
            size -= count[0];
            count[0] = 0;
            keyDenylist.add(key);
          }
        }
      }
    }

    @Override
    public int size()
    {
      return size;
    }

    @Override
    public Iterator<Dataset> iterator()
    {
      return new MyIterator();
    }

    @Override
    public boolean isEmpty()
    {
      return size == 0;
    }

    private class MyIterator implements Iterator<Dataset>
    {
      private Iterator<Dataset> iter;

      private boolean inOverride;

      private int remaining;

      public MyIterator()
      {
        iter = overrideResults.iterator();
        inOverride = true;
        remaining = size;
      }

      @Override
      public void remove()
      {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean hasNext()
      {
        return remaining > 0;
      }

      @Override
      public Dataset next()
      {
        if (remaining == 0) {
          throw new NoSuchElementException();
        }

        --remaining;

        if (inOverride)
        {
          if (iter.hasNext()) {
            return iter.next();
          }
          inOverride = false;
          iter = results.iterator();
        }

        Dataset ds;
        do
        {
          ds = iter.next();
        } while (keyDenylist.contains(ds.getKey()));

        return ds;
      }
    }
  }

}
