/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Datasource, die Daten einer Datenquelle A von Dateien einer anderen Datenquelle B
 * verdecken lässt. Dies funktioniert so, dass Anfragen erst an Datenquelle A
 * gestellt werden und dann für alle Ergebnisdatensätze geprüft wird, ob ein
 * Datensatz (oder mehrere Datensätze) mit gleichem Schlüssel in Datenquelle B ist.
 * Falls dies so ist, werden für diesen Schlüssel nur die Datensätze aus Datenquelle
 * B zurückgeliefert.
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
   * Erzeugt eine neue PreferDatasource.
   *
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser PreferDatasource
   *          bereits vollständig instanziierten Datenquellen.
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser PreferDatasource
   *          enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen (zur Zeit nicht
   *          verwendet).
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
     * Anmerkung: Die folgende Bedingung ist "unnötig" streng, aber um sie
     * aufzuweichen (z.B. Gesamtschema ist immer Schema von bevorzugter Datenquelle)
     * wäre es erforderlich, einen Dataset-Wrapper zu implementieren, der dafür
     * sorgt, dass alle Datasets, die in QueryResults zurück- geliefert werden das
     * selbe Schema haben. Solange dafür keine Notwendigkeit ersichtlich ist, spare
     * ich mir diesen Aufwand.
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
       * Datensätze für die ein Korrekturdatensatz vorliegt, dieaber nicht in
       * overrideResults auftauchen (weil die Korrektur dafür gesorgt hat, dass die
       * Suchbedingung nicht mehr passt) müssen auch mit ihrem Schlüssel auf die
       * Denylist. Deswegen müssen wir diese Datensätze suchen.
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
