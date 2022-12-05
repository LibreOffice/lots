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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.collections4.CollectionUtils;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Datasource, die die Vereinigung 2er Datasources darstellt
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
   * Erzeugt eine neue UnionDatasource.
   *
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser UnionDatasource
   *          bereits vollständig instanziierten Datenquellen.
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser UnionDatasource
   *          enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen (zur Zeit nicht
   *          verwendet).
   */
  public UnionDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc, URL context)
  {
    name = parseConfig(sourceDesc, "NAME", () -> L.m("NAME der Datenquelle fehlt"));
    source1Name = parseConfig(sourceDesc, "SOURCE1", () -> L.m("SOURCE1 der Datenquelle \"%1\" fehlt", name));
    source2Name = parseConfig(sourceDesc, "SOURCE2", () -> L.m("SOURCE2 der Datenquelle \"%1\" fehlt", name));

    source1 = nameToDatasource.get(source1Name);
    source2 = nameToDatasource.get(source2Name);

    if (source1 == null)
      throw new ConfigurationErrorException(L.m("Fehler bei Initialisierung von Datenquelle \"%1\": "
          + "Referenzierte Datenquelle \"%2\" nicht (oder fehlerhaft) definiert", name, source1Name));

    if (source2 == null)
      throw new ConfigurationErrorException(L.m("Fehler bei Initialisierung von Datenquelle \"%1\": "
          + "Referenzierte Datenquelle \"%2\" nicht (oder fehlerhaft) definiert", name, source2Name));

    /*
     * Anmerkung: Die folgende Bedingung ist "unnötig" streng, aber um sie
     * aufzuweichen (z.B. Gesamtschema ist Vereinigung der Schemata) wäre es
     * erforderlich, einen Dataset-Wrapper zu implementieren, der dafür sorgt, dass
     * alle Datasets, die in QueryResults zurück- geliefert werden das selbe Schema
     * haben. Solange dafür keine Notwendigkeit ersichtlich ist, spare ich mir diesen
     * Aufwand.
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
          "Datenquelle \"%1\" fehlen die Spalten: %2 und Datenquelle \"%3\" fehlen die Spalten: %4",
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
